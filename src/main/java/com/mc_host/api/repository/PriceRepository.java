package com.mc_host.api.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.model.plan.AcceptedCurrency;
import com.mc_host.api.model.plan.ContentPrice;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PriceRepository extends BaseRepository {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<Map<String, Long>> CURRENCY_MAP_TYPE =
        new TypeReference<Map<String, Long>>() {};

    public PriceRepository(JdbcTemplate jdbc) { super(jdbc); }

    public void insertPrice(ContentPrice price) {
        try {
            String minorAmountsJson = objectMapper.writeValueAsString(price.minorAmounts());

            upsert("""
            INSERT INTO price_ (
                price_id,
                product_id,
                active,
                minor_amounts)
            VALUES (?, ?, ?, ?::jsonb)
            ON CONFLICT (price_id) DO UPDATE SET
                product_id = EXCLUDED.product_id,
                active = EXCLUDED.active,
                minor_amounts = EXCLUDED.minor_amounts
            """, ps -> {
                ps.setString(1, price.priceId());
                ps.setString(2, price.productId());
                ps.setBoolean(3, price.active());
                ps.setString(4, minorAmountsJson);
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("failed to serialize minor_amounts", e);
        }
    }

    public void deleteProductPrice(String priceId, String productId) {
        execute("DELETE FROM price_ WHERE product_id = ? AND price_id = ?", productId, priceId);
    }

    public List<ContentPrice> selectPricesByProductId(String productId) {
        return selectMany("""
            SELECT
                price_id,
                product_id,
                active,
                minor_amounts
            FROM price_
            WHERE product_id = ?
            ORDER BY active, currency, minor_amount DESC
            """, this::mapPrice, productId);
    }

    public Optional<ContentPrice> selectPrice(String priceId) {
        return selectOne("""
            SELECT
                price_id,
                product_id,
                active,
                minor_amounts
            FROM price_
            WHERE price_id = ?
            """, this::mapPrice, priceId);
    }

    public Optional<String> selectProductId(String priceId) {
        return selectOne("SELECT product_id FROM price_ WHERE price_id = ?",
                (rs, rowNum) -> rs.getString("product_id"), priceId);
    }

    private ContentPrice mapPrice(ResultSet rs, int rowNum) throws SQLException {
        try {
            String jsonbString = rs.getString("minor_amounts");
            Map<String, Long> rawAmounts = objectMapper.readValue(jsonbString, CURRENCY_MAP_TYPE);
            Map<AcceptedCurrency, Long> minorAmounts = rawAmounts.entrySet().stream()
                .collect(Collectors.toMap(
                    entry -> AcceptedCurrency.fromCode(entry.getKey()),
                    Map.Entry::getValue
                ));

            return new ContentPrice(
                rs.getString("price_id"),
                rs.getString("product_id"),
                rs.getBoolean("active"),
                minorAmounts);
        } catch (Exception e) {
            throw new SQLException("failed to parse minor_amounts jsonb", e);
        }
    }
}
