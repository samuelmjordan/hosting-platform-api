package com.mc_host.api.persistence;

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mc_host.api.model.entity.SubscriptionEntity;

@Service
public class SubscriptionPersistenceService {

    private final JdbcTemplate jdbcTemplate;

    public SubscriptionPersistenceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertSubscription(SubscriptionEntity subscriptionEntity) {
        try {
            jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement("""
                    INSERT INTO subscriptions (
                        subscription_id, 
                        customer_id, 
                        status, 
                        price_id, 
                        current_period_end, 
                        current_period_start, 
                        cancel_at_period_end
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (subscription_id) DO UPDATE SET
                        customer_id = EXCLUDED.customer_id,
                        status = EXCLUDED.status,
                        price_id = EXCLUDED.price_id,
                        current_period_end = EXCLUDED.current_period_end,
                        current_period_start = EXCLUDED.current_period_start,
                        cancel_at_period_end = EXCLUDED.cancel_at_period_end
                    """);
                ps.setString(1, subscriptionEntity.subscriptionId());
                ps.setString(2, subscriptionEntity.customerId());
                ps.setString(3, subscriptionEntity.status());
                ps.setString(4, subscriptionEntity.priceId());
                ps.setTimestamp(5, Timestamp.from(subscriptionEntity.currentPeriodEnd()), 
                              java.util.Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC)));
                ps.setTimestamp(6, Timestamp.from(subscriptionEntity.currentPeriodStart()),
                              java.util.Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC)));
                
                ps.setBoolean(7, subscriptionEntity.cancelAtPeriodEnd());
                
                return ps;
            });
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to save subscription to database: " + e.getMessage(), e);
        }
    }

    public void deleteCustomerSubscriptions(Set<String> subscriptionIds, String customerId) {
        try {
            jdbcTemplate.update(
                """
                DELETE FROM subscriptions 
                WHERE customer_id = ? 
                AND subscription_id = ANY(?)
                """,
                customerId,
                createArrayOf("text", subscriptionIds.toArray())
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to delete subscriptions from database: " + e.getMessage(), e);
        }
    }

    public List<SubscriptionEntity> selectSubscriptionsByCustomerId(String customerId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT 
                    subscription_id,
                    customer_id,
                    status,
                    price_id,
                    current_period_end,
                    current_period_start,
                    cancel_at_period_end
                FROM subscriptions 
                WHERE customer_id = ?
                ORDER BY current_period_start DESC
                """,
                (rs, rowNum) -> new SubscriptionEntity(
                    rs.getString("subscription_id"),
                    rs.getString("customer_id"),
                    rs.getString("status"),
                    rs.getString("price_id"),
                    rs.getTimestamp("current_period_end", 
                        java.util.Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC))).toInstant(),
                    rs.getTimestamp("current_period_start", 
                        java.util.Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC))).toInstant(),
                    rs.getBoolean("cancel_at_period_end")
                ),
                customerId
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to fetch subscriptions for customer: " + e.getMessage(), e);
        }
    }

    private Array createArrayOf(String typeName, Object[] elements) {
        try {
            return jdbcTemplate.getDataSource()
                .getConnection()
                .createArrayOf(typeName, elements);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create SQL array: " + e.getMessage(), e);
        }
    }
}