package com.mc_host.api.repository;

import com.mc_host.api.model.plan.AcceptedCurrency;
import com.mc_host.api.model.stripe.SubscriptionStatus;
import com.mc_host.api.model.subscription.ContentSubscription;
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

    public void upsertSubscription(ContentSubscription sub) {
        upsert("""
            INSERT INTO subscription_ (
                subscription_id,
                customer_id,
                status_,
                price_id,
                currency,
                current_period_end,
                current_period_start,
                cancel_at_period_end,
                subdomain)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (subscription_id) DO UPDATE SET
                customer_id = EXCLUDED.customer_id,
                status_ = EXCLUDED.status_,
                price_id = EXCLUDED.price_id,
                currency = EXCLUDED.currency,
                current_period_end = EXCLUDED.current_period_end,
                current_period_start = EXCLUDED.current_period_start,
                cancel_at_period_end = EXCLUDED.cancel_at_period_end,
                subdomain = EXCLUDED.subdomain
            """,
            ps -> setSubscriptionParams(ps, sub)
        );
    }

    public List<ContentSubscription> selectSubscriptionsByCustomerId(String customerId) {
        return selectMany("""
            SELECT subscription_id,
                customer_id,
                status_,
                price_id,
                currency,
                current_period_end,
                current_period_start,
                cancel_at_period_end,
                subdomain
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
                currency,
                current_period_end,
                current_period_start,
                cancel_at_period_end,
                subdomain
            FROM subscription_
            WHERE subscription_id = ?
            """,
            this::mapSubscription,
            subId
        );
    }

    public Optional<String> selectSubscriptionOwnerUserId(String subscriptionId) {
        return selectOne("""
            SELECT
                clerk_id
            FROM subscription_
            JOIN user_ on user_.customer_id = subscription_.customer_id
            WHERE subscription_id = ?
            """,
            (rs, rowNum) -> rs.getString("clerk_id"),
            subscriptionId
        );
    }

    public int updateUserCurrencyFromSubscription(String customerId) {
        return execute("""
            UPDATE user_ SET currency = COALESCE(
                (SELECT currency
                FROM subscription_
                WHERE subscription_.customer_id = ? AND subscription_.status_ = 'active'
                LIMIT 1),
                'XXX')
            WHERE customer_id = ?
            """,
            customerId,
            customerId
        );
    }

    public Boolean domainExists(String subdomain) {
        return selectOne("SELECT 1 FROM subscription_ WHERE ? = subdomain",
            (rs, rowNum) -> true, subdomain)
            .isPresent();
    }

    public int updateSubdomain(String subscriptionId) {
        return execute("""
            UPDATE subscription_
            SET subdomain = ?
            WHERE subscription_id = ?
            """,
            subscriptionId
        );
    }

    public int updateSubdomainIfAvailable(String newSubdomain, String subscriptionId) {
        return execute("""
        UPDATE subscription_
        SET subdomain = ?
        WHERE subscription_id = ?
        AND NOT EXISTS (SELECT 1 FROM subscription_ WHERE subdomain = ?)
        """,
            newSubdomain,
            subscriptionId,
            newSubdomain);
    }



    private void setSubscriptionParams(PreparedStatement ps, ContentSubscription sub) throws SQLException {
        ps.setString(1, sub.subscriptionId());
        ps.setString(2, sub.customerId());
        ps.setString(3, sub.status().toString());
        ps.setString(4, sub.priceId());
        ps.setString(5, sub.currency().name());
        ps.setTimestamp(6, Timestamp.from(sub.currentPeriodEnd()),
                Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC)));
        ps.setTimestamp(7, Timestamp.from(sub.currentPeriodStart()),
                Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC)));
        ps.setBoolean(8, sub.cancelAtPeriodEnd());
        ps.setString(9, sub.subdomain());
    }

    private ContentSubscription mapSubscription(ResultSet rs, int rowNum) throws SQLException {
        return new ContentSubscription(
            rs.getString("subscription_id"),
            rs.getString("customer_id"),
            SubscriptionStatus.fromString(rs.getString("status_")),
            rs.getString("price_id"),
            AcceptedCurrency.fromCode(rs.getString("currency")),
            rs.getTimestamp("current_period_end",
                    Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC))).toInstant(),
            rs.getTimestamp("current_period_start",
                    Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC))).toInstant(),
            rs.getBoolean("cancel_at_period_end"),
            rs.getString("subdomain"));
    }
}