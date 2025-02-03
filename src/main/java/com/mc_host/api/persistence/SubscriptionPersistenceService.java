package com.mc_host.api.persistence;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mc_host.api.model.SubscriptionEntity;

@Service
public class SubscriptionPersistenceService {

    private final JdbcTemplate jdbcTemplate;

    public SubscriptionPersistenceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertSubscription(SubscriptionEntity subscriptionEntity) {
        try {
            jdbcTemplate.update(
                """
                INSERT INTO subscriptions (
                    subscription_id, 
                    customer_id, 
                    status, 
                    price_id, 
                    current_period_end, 
                    current_period_start, 
                    cancel_at_period_end, 
                    payment_method
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (subscription_id) DO UPDATE SET
                    customer_id = EXCLUDED.customer_id,
                    status = EXCLUDED.status,
                    price_id = EXCLUDED.price_id,
                    current_period_end = EXCLUDED.current_period_end,
                    current_period_start = EXCLUDED.current_period_start,
                    cancel_at_period_end = EXCLUDED.cancel_at_period_end,
                    payment_method = EXCLUDED.payment_method
                """,
                subscriptionEntity.subscriptionId(),
                subscriptionEntity.customerId(),
                subscriptionEntity.status(),
                subscriptionEntity.priceId(),
                subscriptionEntity.currentPeriodEnd(),
                subscriptionEntity.currentPeriodStart(),
                subscriptionEntity.cancelAtPeriodEnd(),
                subscriptionEntity.paymentMethod()
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to save subscription to database: " + e.getMessage(), e);
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
                    cancel_at_period_end,
                    payment_method
                FROM subscriptions 
                WHERE customer_id = ?
                ORDER BY current_period_start DESC
                """,
                // RowMapper to convert database rows to SubscriptionEntity
                (rs, rowNum) -> new SubscriptionEntity(
                    rs.getString("subscription_id"),
                    rs.getString("customer_id"),
                    rs.getString("status"),
                    rs.getString("price_id"),
                    rs.getTimestamp("current_period_end").toInstant(),
                    rs.getTimestamp("current_period_start").toInstant(),
                    rs.getBoolean("cancel_at_period_end"),
                    rs.getString("payment_method")
                ),
                customerId
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to fetch subscriptions for customer: " + e.getMessage(), e);
        }
    }
    
}

