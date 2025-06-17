package com.mc_host.api.repository;

import com.mc_host.api.model.stripe.SubscriptionStatus;
import com.mc_host.api.model.subscription.ContentSubscription;
import com.mc_host.api.model.subscription.MarketingRegion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

@Service
public class SubscriptionRepository extends BaseRepository {

    public SubscriptionRepository(JdbcTemplate jdbc) { super(jdbc); }

    public void insertSubscription(ContentSubscription sub) {
        upsert("""
            INSERT INTO subscription_ (
                subscription_id,
                customer_id,
                status_,
                price_id,
                current_period_end,
                current_period_start,
                cancel_at_period_end,
                initial_region)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            ps -> setSubscriptionParams(ps, sub));
    }

    public void updateSubscription(ContentSubscription sub) {
        upsert("""
            UPDATE subscription_ SET
                customer_id = ?,
                status_ = ?,
                price_id = ?,
                current_period_end = ?,
                current_period_start = ?,
                cancel_at_period_end = ?,
                initial_region = ?
            WHERE subscription_id = ?
            """, ps -> {
            setSubscriptionParams(ps, sub);
            ps.setString(8, sub.subscriptionId());
        });
    }

    public void upsertSubscription(ContentSubscription sub) {
        upsert("""
            INSERT INTO subscription_ (
                subscription_id,
                customer_id,
                status_,
                price_id,
                current_period_end,
                current_period_start,
                cancel_at_period_end,
                initial_region)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (subscription_id) DO UPDATE SET
                customer_id = EXCLUDED.customer_id,
                status_ = EXCLUDED.status_,
                price_id = EXCLUDED.price_id,
                current_period_end = EXCLUDED.current_period_end,
                current_period_start = EXCLUDED.current_period_start,
                cancel_at_period_end = EXCLUDED.cancel_at_period_end,
                initial_region = EXCLUDED.initial_region
            """,
            ps -> setSubscriptionParams(ps, sub)
        );
    }

    public void deleteSubscription(String subId) {
        execute("DELETE FROM subscription_ WHERE subscription_id = ?", subId);
    }

    public List<ContentSubscription> selectSubscriptionsByCustomerId(String customerId) {
        return selectMany("""
            SELECT subscription_id,
                customer_id,
                status_,
                price_id,
                current_period_end,
                current_period_start,
                cancel_at_period_end,
                initial_region
            FROM subscription_ 
            WHERE customer_id = ?
            ORDER BY status_, current_period_start DESC
            """, this::mapSubscription, customerId);
    }

    public Optional<ContentSubscription> selectSubscription(String subId) {
        return selectOne("""
            SELECT subscription_id,
                customer_id,
                status_,
                price_id,
                current_period_end,
                current_period_start,
                cancel_at_period_end,
                initial_region
            FROM subscription_
            WHERE subscription_id = ?
            """,
            this::mapSubscription,
            subId
        );
    }

    public int updateUserCurrencyFromSubscription(String customerId) {
        return execute("""
            UPDATE user_ SET currency = COALESCE(
                (SELECT price_.currency
                FROM subscription_
                JOIN price_ ON subscription_.price_id = price_.price_id
                WHERE subscription_.customer_id = ? AND subscription_.status_ = 'active'
                LIMIT 1),
                'XXX')
            WHERE customer_id = ?
            """,
            customerId,
            customerId
        );
    }

    private void setSubscriptionParams(PreparedStatement ps, ContentSubscription sub) throws SQLException {
        ps.setString(1, sub.subscriptionId());
        ps.setString(2, sub.customerId());
        ps.setString(3, sub.status().toString());
        ps.setString(4, sub.priceId());
        ps.setTimestamp(5, Timestamp.from(sub.currentPeriodEnd()),
                Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC)));
        ps.setTimestamp(6, Timestamp.from(sub.currentPeriodStart()),
                Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC)));
        ps.setBoolean(7, sub.cancelAtPeriodEnd());
        ps.setString(8, sub.initialRegion().name());
    }

    private ContentSubscription mapSubscription(ResultSet rs, int rowNum) throws SQLException {
        return new ContentSubscription(
            rs.getString("subscription_id"),
            rs.getString("customer_id"),
            SubscriptionStatus.fromString(rs.getString("status_")),
            rs.getString("price_id"),
            rs.getTimestamp("current_period_end",
                    Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC))).toInstant(),
            rs.getTimestamp("current_period_start",
                    Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC))).toInstant(),
            rs.getBoolean("cancel_at_period_end"),
            MarketingRegion.valueOf(rs.getString("initial_region")));
    }
}