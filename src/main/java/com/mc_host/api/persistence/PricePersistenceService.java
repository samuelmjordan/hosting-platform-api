package com.mc_host.api.persistence;

import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
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

    @Cacheable(value = "product-prices", key = "#priceEntity.productId()", unless = "#result.isEmpty()")
    public void insertPrice(PriceEntity priceEntity) {
        try {
            jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement("""
                    INSERT INTO prices (
                        price_id, 
                        product_id,
                        spec_id,
                        active, 
                        currency, 
                        minor_amount
                    )
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT (price_id) DO UPDATE SET
                        price_id = EXCLUDED.price_id,
                        product_id = EXCLUDED.product_id,
                        spec_id = EXCLUDED.spec_id,
                        active = EXCLUDED.active,
                        currency = EXCLUDED.currency,
                        minor_amount = EXCLUDED.minor_amount
                    """);
                ps.setString(1, priceEntity.priceId());
                ps.setString(2, priceEntity.productId());
                ps.setString(3, priceEntity.specId());
                ps.setBoolean(4, priceEntity.active());
                ps.setString(5, priceEntity.currency().name());
                ps.setLong(6, priceEntity.minorAmount());
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
                DELETE FROM prices 
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
                    spec_id,
                    active, 
                    currency, 
                    minor_amount
                FROM prices 
                WHERE product_id = ?
                ORDER BY spec_id, currency DESC
                """,
                (rs, rowNum) -> new PriceEntity(
                    rs.getString("price_id"),
                    rs.getString("product_id"),
                    rs.getString("spec_id"),
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
