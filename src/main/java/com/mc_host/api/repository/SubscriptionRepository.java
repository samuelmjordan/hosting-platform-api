package com.mc_host.api.repository;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import org.postgresql.util.PGobject;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.model.AcceptedCurrency;
import com.mc_host.api.model.entity.ContentPrice;
import com.mc_host.api.model.entity.ContentSubscription;
import com.mc_host.api.model.entity.SubscriptionUserMetadata;

@Service
public class SubscriptionRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public SubscriptionRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void insertSubscriptionWithMetadata(ContentSubscription subscriptionEntity, SubscriptionUserMetadata subscriptionUserMetadata) {
        insertSubscription(subscriptionEntity);
        upsertSubscriptionMetadata(subscriptionUserMetadata);
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
                        cancel_at_period_end,
                        metadata
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (subscription_id) DO UPDATE SET
                        customer_id = EXCLUDED.customer_id,
                        status_ = EXCLUDED.status_,
                        price_id = EXCLUDED.price_id,
                        current_period_end = EXCLUDED.current_period_end,
                        current_period_start = EXCLUDED.current_period_start,
                        cancel_at_period_end = EXCLUDED.cancel_at_period_end,
                        metadata = EXCLUDED.metadata
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
                
                PGobject jsonObject = new PGobject();
                jsonObject.setType("jsonb");
                try {
                    Map<String, String> metadata = subscriptionEntity.metadata();
                    jsonObject.setValue(metadata != null ? objectMapper.writeValueAsString(metadata) : "{}");
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to serialize metadata: " + e.getMessage(), e);
                }
                ps.setObject(8, jsonObject);
                
                return ps;
            });
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to save subscription to database: " + e.getMessage(), e);
        }
    }

    public void upsertSubscriptionMetadata(SubscriptionUserMetadata subscriptionUserMetadata) {
        try {
            jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement("""
                    INSERT INTO subscription_user_metadata_ (
                        subscription_id, 
                        title, 
                        caption
                    )
                    VALUES (?, ?, ?)
                    ON CONFLICT (subscription_id) DO UPDATE SET
                        subscription_id = EXCLUDED.subscription_id,
                        title = EXCLUDED.title,
                        caption = EXCLUDED.caption
                    """);
                ps.setString(1, subscriptionUserMetadata.subscriptionId());
                ps.setString(2, subscriptionUserMetadata.title());
                ps.setString(3, subscriptionUserMetadata.caption());
                return ps;
            });
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to save subscription metadata to database: " + e.getMessage(), e);
        }
    }

        public void updateSubscriptionTitle(String subscriptionId, String title) {
        try {
            jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement("""
                    UPDATE subscription_user_metadata_ 
                    SET title = ?
                    WHERE subscription_id = ?;
                    """);
                ps.setString(1, title);
                ps.setString(2, subscriptionId);
                return ps;
            });
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to update subscription title to database: " + e.getMessage(), e);
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
                    cancel_at_period_end,
                    metadata
                FROM subscription_
                WHERE customer_id = ?
                ORDER BY status_, current_period_start DESC
                """,
                (rs, rowNum) -> {
                    Map<String, String> metadata = new HashMap<>();
                    try {
                        String metadataJson = rs.getString("metadata");
                        if (metadataJson != null && !metadataJson.isEmpty() && !metadataJson.equals("{}")) {
                            metadata = objectMapper.readValue(metadataJson, 
                                new TypeReference<Map<String, String>>() {});
                        }
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Failed to deserialize metadata: " + e.getMessage(), e);
                    }
                    
                    return new ContentSubscription(
                        rs.getString("subscription_id"),
                        rs.getString("customer_id"),
                        rs.getString("status_"),
                        rs.getString("price_id"),
                        rs.getTimestamp("current_period_end", 
                            java.util.Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC))).toInstant(),
                        rs.getTimestamp("current_period_start", 
                            java.util.Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC))).toInstant(),
                        rs.getBoolean("cancel_at_period_end"),
                        metadata
                    );
                },
                customerId
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to fetch subscriptions for customer: " + e.getMessage(), e);
        }
    }

    public Optional<SubscriptionUserMetadata> selectSubscriptionUserMetadataBySubscriptionId(String subscriptionId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT 
                    subscription_id,
                    title,
                    caption
                FROM subscription_user_metadata_
                WHERE subscription_id = ?
                """,
                (rs, rowNum) -> new SubscriptionUserMetadata(
                    rs.getString("subscription_id"),
                    rs.getString("title"),
                    rs.getString("caption")
                ),
                subscriptionId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to fetch subscription metadata for subscription: " + subscriptionId, e);
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