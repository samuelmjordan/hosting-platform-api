package com.mc_host.api.repository;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.List;
import java.util.TimeZone;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mc_host.api.model.entity.ContentSubscription;

@Service
public class SubscriptionRepository {

    private final JdbcTemplate jdbcTemplate;

    public SubscriptionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertSubscription(ContentSubscription subscriptionEntity) {
        try {
            jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement("""
                    INSERT INTO subscription_ (
                        subscription_id, 
                        customer_id, 
                        status_, 
                        price_id, 
                        current_period_end, 
                        current_period_start, 
                        cancel_at_period_end
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (subscription_id) DO UPDATE SET
                        customer_id = EXCLUDED.customer_id,
                        status_ = EXCLUDED.status_,
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

    public void deleteCustomerSubscription(String subscriptionId, String customerId) {
        try {
            jdbcTemplate.update(
                """
                DELETE FROM subscription_
                WHERE customer_id = ? 
                AND subscription_id = ?
                """,
                customerId,
                subscriptionId
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to delete subscriptions from database: " + e.getMessage(), e);
        }
    }

    public List<ContentSubscription> selectSubscriptionsByCustomerId(String customerId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT 
                    subscription_id,
                    customer_id,
                    status_,
                    price_id,
                    current_period_end,
                    current_period_start,
                    cancel_at_period_end
                FROM subscription_
                WHERE customer_id = ?
                ORDER BY status_, current_period_start DESC
                """,
                (rs, rowNum) -> new ContentSubscription(
                    rs.getString("subscription_id"),
                    rs.getString("customer_id"),
                    rs.getString("status_"),
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

    public int updateUserCurrencyFromSubscription(String customerId) {
        try {
            return jdbcTemplate.update(
                """
                UPDATE user_
                SET currency = COALESCE(
                    (SELECT price_.currency
                    FROM subscription_
                    JOIN price_ ON subscription_.price_id = price_.price_id
                    WHERE subscription_.customer_id = ?
                    AND subscription_.status_ = 'active'
                    LIMIT 1),
                    'XXX'
                )
                WHERE customer_id = ?
                """,
                customerId,
                customerId
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to update currency for customer ID: " + customerId, e);
        }
     }
}