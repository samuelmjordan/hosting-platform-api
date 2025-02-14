package com.mc_host.api.persistence;

import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mc_host.api.model.Currency;
import com.mc_host.api.model.entity.PriceEntity;

@Service
public class PricePersistenceService {

    private final JdbcTemplate jdbcTemplate;

    public PricePersistenceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertPrice(PriceEntity priceEntity) {
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

    public void deleteProductPrices(Set<String> priceIds, String productId) {
        try {
            jdbcTemplate.update(
                """
                DELETE FROM price_
                WHERE product_id = ? 
                AND price_id = ANY(?)
                """,
                productId,
                createArrayOf("text", priceIds.toArray())
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to delete prices from database: " + e.getMessage(), e);
        }
    }

    public List<PriceEntity> selectPricesByProductId(String productId) {
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
                (rs, rowNum) -> new PriceEntity(
                    rs.getString("price_id"),
                    rs.getString("product_id"),
                    rs.getBoolean("active"),
                    Currency.fromCode(rs.getString("currency")),
                    rs.getLong("minor_amount")
                ),
                productId
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to fetch prices for product: " + e.getMessage(), e);
        }
    }


    private Array createArrayOf(String typeName, Object[] elements) {
        try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {
            return conn.createArrayOf(typeName, elements);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create SQL array: " + e.getMessage(), e);
        }
    }   
}
