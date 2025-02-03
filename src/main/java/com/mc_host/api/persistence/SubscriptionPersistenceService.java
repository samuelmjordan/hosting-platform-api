package com.mc_host.api.persistence;

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
    
}

