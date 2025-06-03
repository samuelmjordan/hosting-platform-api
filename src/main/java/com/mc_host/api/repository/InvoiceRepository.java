package com.mc_host.api.repository;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mc_host.api.model.plan.AcceptedCurrency;
import com.mc_host.api.model.stripe.CustomerInvoice;

@Service
public class InvoiceRepository {

    private final JdbcTemplate jdbcTemplate;

    public InvoiceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertInvoice(CustomerInvoice invoice) {
        try {
            jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement("""
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
                    """);
                ps.setString(1, invoice.invoiceId());
                ps.setString(2, invoice.customerId());
                ps.setString(3, invoice.subscriptionId());
                ps.setString(4, invoice.invoiceNumber());
                ps.setBoolean(5, invoice.paid());
                ps.setString(6, invoice.paymentMethod() != null ? invoice.paymentMethod() : null);
                ps.setString(7, invoice.collectionMethod() != null ? invoice.collectionMethod() : null);
                ps.setString(8, invoice.currency().name());
                ps.setLong(9, invoice.minorAmount());
                ps.setTimestamp(10, Timestamp.from(invoice.createdAt()), 
                              java.util.Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC)));
                ps.setString(11, invoice.link());
                return ps;
            });
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to save invoice to database: " + e.getMessage(), e);
        }
    }

    public List<CustomerInvoice> selectInvoicesByCustomerId(String customerId) {
        try {
            return jdbcTemplate.query(
                """
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
                FROM invoice_
                WHERE customer_id = ?
                ORDER BY invoice_created_at, invoice_id DESC
                """,
                (rs, rowNum) -> new CustomerInvoice(
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
                        java.util.Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC))).toInstant(),
                    rs.getString("link")
                ),
                customerId
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to fetch invoices for customer: " + customerId, e);
        }
    }

    public Optional<CustomerInvoice> selectInvoice(String invoiceId) {
        try {
            return jdbcTemplate.query(
                """
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
                FROM invoice_
                WHERE invoice_id = ?
                ORDER BY invoice_created_at, invoice_id DESC
                """,
                (rs, rowNum) -> new CustomerInvoice(
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
                        java.util.Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC))).toInstant(),
                    rs.getString("link")
                ),
                invoiceId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to fetch invoice: " + invoiceId, e);
        }
    }
}
