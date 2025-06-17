package com.mc_host.api.repository;

import com.mc_host.api.model.plan.AcceptedCurrency;
import com.mc_host.api.model.plan.ContentPrice;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Service
public class PriceRepository extends BaseRepository {

    public PriceRepository(JdbcTemplate jdbc) { super(jdbc); }

    public void insertPrice(ContentPrice price) {
        upsert("""
            INSERT INTO price_ (
                price_id,
                product_id,
                active,
                currency,
                minor_amount)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (price_id) DO UPDATE SET
                price_id = EXCLUDED.price_id,
                product_id = EXCLUDED.product_id,
                active = EXCLUDED.active,
                currency = EXCLUDED.currency,
                minor_amount = EXCLUDED.minor_amount
            """, ps -> {
            ps.setString(1, price.priceId());
            ps.setString(2, price.productId());
            ps.setBoolean(3, price.active());
            ps.setString(4, price.currency().name());
            ps.setLong(5, price.minorAmount());
        });
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
                currency,
                minor_amount
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
                currency,
                minor_amount
            FROM price_
            WHERE price_id = ?
            """, this::mapPrice, priceId);
    }

    public Optional<String> selectProductId(String priceId) {
        return selectOne("SELECT product_id FROM price_ WHERE price_id = ?",
                (rs, rowNum) -> rs.getString("product_id"), priceId);
    }

    private ContentPrice mapPrice(ResultSet rs, int rowNum) throws SQLException {
        return new ContentPrice(
                rs.getString("price_id"),
                rs.getString("product_id"),
                rs.getBoolean("active"),
                AcceptedCurrency.fromCode(rs.getString("currency")),
                rs.getLong("minor_amount"));
    }
}
