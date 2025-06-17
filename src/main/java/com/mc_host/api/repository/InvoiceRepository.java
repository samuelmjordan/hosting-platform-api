package com.mc_host.api.repository;

import com.mc_host.api.model.plan.AcceptedCurrency;
import com.mc_host.api.model.stripe.CustomerInvoice;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

@Service
public class InvoiceRepository extends BaseRepository {

    public InvoiceRepository(JdbcTemplate jdbc) { super(jdbc); }

    public void insertInvoice(CustomerInvoice invoice) {
        upsert("""
            INSERT INTO invoice_ (
                invoice_id,
                customer_id,
                subscription_id,
                invoice_number,
                paid,
                payment_method,
                collection_method,
                currency,
                minor_amount,
                invoice_created_at,
                link
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (invoice_id) DO UPDATE SET
                customer_id = EXCLUDED.customer_id,
                subscription_id = EXCLUDED.subscription_id,
                invoice_number = EXCLUDED.invoice_number,
                paid = EXCLUDED.paid,
                payment_method = EXCLUDED.payment_method,
                collection_method = EXCLUDED.collection_method,
                currency = EXCLUDED.currency,
                minor_amount = EXCLUDED.minor_amount,
                invoice_created_at = EXCLUDED.invoice_created_at,
                link = EXCLUDED.link
            """, ps -> {
            ps.setString(1, invoice.invoiceId());
            ps.setString(2, invoice.customerId());
            ps.setString(3, invoice.subscriptionId());
            ps.setString(4, invoice.invoiceNumber());
            ps.setBoolean(5, invoice.paid());
            ps.setString(6, invoice.paymentMethod());
            ps.setString(7, invoice.collectionMethod());
            ps.setString(8, invoice.currency().name());
            ps.setLong(9, invoice.minorAmount());
            ps.setTimestamp(10, Timestamp.from(invoice.createdAt()),
                    Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC)));
            ps.setString(11, invoice.link());
        });
    }

    public List<CustomerInvoice> selectInvoicesByCustomerId(String customerId) {
        return selectMany("""
            SELECT
                invoice_id,
                customer_id,
                subscription_id,
                invoice_number,
                paid,
                payment_method,
                collection_method,
                currency,
                minor_amount,
                invoice_created_at,
                link
            FROM invoice_ WHERE customer_id = ?
            ORDER BY invoice_created_at, invoice_id DESC
            """,
            this::mapInvoice,
            customerId
        );
    }

    public Optional<CustomerInvoice> selectInvoice(String invoiceId) {
        return selectOne("""
            SELECT
                invoice_id,
                customer_id,
                subscription_id,
                invoice_number,
                paid,
                payment_method,
                collection_method,
                currency,
                minor_amount, 
                invoice_created_at,
                link
            FROM invoice_ WHERE invoice_id = ?
            """,
                this::mapInvoice,
                invoiceId
        );
    }

    private CustomerInvoice mapInvoice(ResultSet rs, int rowNum) throws SQLException {
        return new CustomerInvoice(
            rs.getString("invoice_id"),
            rs.getString("customer_id"),
            rs.getString("subscription_id"),
            rs.getString("invoice_number"),
            rs.getBoolean("paid"),
            rs.getString("payment_method"),
            rs.getString("collection_method"),
            AcceptedCurrency.fromCode(rs.getString("currency")),
            rs.getLong("minor_amount"),
            rs.getTimestamp("invoice_created_at",
                    Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC))).toInstant(),
            rs.getString("link"));
    }
}