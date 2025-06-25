package com.mc_host.api.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.model.stripe.CustomerPaymentMethod;
import com.mc_host.api.model.stripe.PaymentMethodType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentMethodRepository extends BaseRepository {
    private final ObjectMapper objectMapper;

    public PaymentMethodRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        super(jdbc);
        this.objectMapper = objectMapper;
    }

    public void upsertPaymentMethod(CustomerPaymentMethod pm) {
        upsert("""
            INSERT INTO payment_method_ (
                payment_method_id,
                customer_id,
                payment_method_type,
                display_name,
                payment_data,
                is_active,
                is_default)
            VALUES (?, ?, ?, ?, ?::jsonb, ?, ?)
            ON CONFLICT (payment_method_id) DO UPDATE SET
                customer_id = EXCLUDED.customer_id,
                payment_method_type = EXCLUDED.payment_method_type,
                display_name = EXCLUDED.display_name,
                payment_data = EXCLUDED.payment_data,
                is_active = EXCLUDED.is_active,
                is_default = EXCLUDED.is_default
            """, ps -> {
            ps.setString(1, pm.paymentMethodId());
            ps.setString(2, pm.customerId());
            ps.setString(3, pm.paymentMethodType().name().toLowerCase());
            ps.setString(4, pm.displayName());
            ps.setString(5, pm.paymentData().toString());
            ps.setBoolean(6, pm.isActive());
            ps.setBoolean(7, pm.isDefault());
        });
    }

    public void deletePaymentMethodsForCustomer(String customerId) {
        execute("DELETE FROM payment_method_ WHERE customer_id = ?", customerId);
    }

    public Optional<String> selectPaymentMethodClerkId(String paymentMethodId) {
        return selectOne("""
            SELECT
                clerk_id
            FROM payment_method_
            JOIN user_ on user_.customer_id = payment_method_.customer_id
            WHERE payment_method_id = ?
            AND is_active = true
            ORDER BY is_default DESC, created_at DESC
            """,
            (rs, rowNum) -> rs.getString("clerk_id"),
            paymentMethodId
        );
    }

    public Optional<String> selectPaymentMethodCustomerId(String paymentMethodId) {
        return selectOne("""
            SELECT
                customer_id
            FROM payment_method_
            WHERE payment_method_id = ?
            AND is_active = true
            ORDER BY is_default DESC, created_at DESC
            """,
            (rs, rowNum) -> rs.getString("customer_id"),
            paymentMethodId
        );
    }

    public List<CustomerPaymentMethod> selectPaymentMethodsByCustomerId(String customerId) {
        return selectMany("""
            SELECT
                payment_method_id,
                customer_id,
                payment_method_type,
                display_name,
                payment_data,
                is_active, is_default
            FROM payment_method_
            WHERE customer_id = ?
            AND is_active = true
            ORDER BY is_default DESC, created_at DESC
            """,
            this::mapPaymentMethod,
            customerId
        );
    }

    private CustomerPaymentMethod mapPaymentMethod(ResultSet rs, int rowNum) throws SQLException {
        JsonNode paymentData;
        try {
            paymentData = objectMapper.readTree(rs.getString("payment_data"));
        } catch (Exception e) {
            throw new RuntimeException("failed to parse payment_data json", e);
        }

        return new CustomerPaymentMethod(
                rs.getString("payment_method_id"),
                rs.getString("customer_id"),
                PaymentMethodType.valueOf(rs.getString("payment_method_type").toUpperCase()),
                rs.getString("display_name"),
                paymentData,
                rs.getBoolean("is_active"),
                rs.getBoolean("is_default"));
    }
}
