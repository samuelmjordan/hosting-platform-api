package com.mc_host.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mc_host.api.model.AcceptedCurrency;
import com.mc_host.api.model.entity.ContentPrice;

@Service
public class PriceRepository {

    private final JdbcTemplate jdbcTemplate;

    public PriceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertPrice(ContentPrice priceEntity) {
        try {
            jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement("""
                    INSERT INTO price_ (
                        price_id, 
                        product_id,
                        active, 
                        currency, 
                        minor_amount
                    )
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT (price_id) DO UPDATE SET
                        price_id = EXCLUDED.price_id,
                        product_id = EXCLUDED.product_id,
                        active = EXCLUDED.active,
                        currency = EXCLUDED.currency,
                        minor_amount = EXCLUDED.minor_amount
                    """);
                ps.setString(1, priceEntity.priceId());
                ps.setString(2, priceEntity.productId());
                ps.setBoolean(3, priceEntity.active());
                ps.setString(4, priceEntity.currency().name());
                ps.setLong(5, priceEntity.minorAmount());
                return ps;
            });
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to save price to database: " + e.getMessage(), e);
        }
    }

    public void deleteProductPrice(String priceId, String productId) {
        try {
            jdbcTemplate.update(
                """
                DELETE FROM price_
                WHERE product_id = ? 
                AND price_id = ?
                """,
                productId,
                priceId
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to delete price from database: " + e.getMessage(), e);
        }
    }

    public List<ContentPrice> selectPricesByProductId(String productId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT 
                    price_id, 
                    product_id,
                    active, 
                    currency, 
                    minor_amount
                FROM price_
                WHERE product_id = ?
                ORDER BY active, currency, minor_amount DESC
                """,
                (rs, rowNum) -> new ContentPrice(
                    rs.getString("price_id"),
                    rs.getString("product_id"),
                    rs.getBoolean("active"),
                    AcceptedCurrency.fromCode(rs.getString("currency")),
                    rs.getLong("minor_amount")
                ),
                productId
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to fetch prices for product: " + e.getMessage(), e);
        }
    }

    public Optional<ContentPrice> selectPrice(String priceId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT 
                    price_id, 
                    product_id,
                    active, 
                    currency, 
                    minor_amount
                FROM price_
                WHERE price_id = ?
                """,
                (rs, rowNum) -> new ContentPrice(
                    rs.getString("price_id"),
                    rs.getString("product_id"),
                    rs.getBoolean("active"),
                    AcceptedCurrency.fromCode(rs.getString("currency")),
                    rs.getLong("minor_amount")
                ),
                priceId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to fetch prices for product: " + e.getMessage(), e);
        }
    }

    public Optional<String> selectProductId(String priceId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT product_id
                FROM price_
                WHERE price_id = ?
                """,
                (rs, rowNum) -> rs.getString("product_id"),
                priceId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to fetch prices for product: " + e.getMessage(), e);
        }
    }
}
