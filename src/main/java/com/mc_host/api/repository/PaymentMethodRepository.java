package com.mc_host.api.repository;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.model.stripe.CustomerPaymentMethod;
import com.mc_host.api.model.stripe.PaymentMethodType;

@Service
public class PaymentMethodRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public PaymentMethodRepository(
        JdbcTemplate jdbcTemplate,
        ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void upsertPaymentMethod(CustomerPaymentMethod paymentMethod) {
        try {
            jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement("""
                    INSERT INTO payment_method_ (
                        payment_method_id, 
                        customer_id, 
                        payment_method_type, 
                        display_name, 
                        payment_data, 
                        is_active, 
                        is_default
                    ) VALUES (?, ?, ?, ?, ?::jsonb, ?, ?)
                    ON CONFLICT (payment_method_id) DO UPDATE SET
                        customer_id = EXCLUDED.customer_id,
                        payment_method_type = EXCLUDED.payment_method_type,
                        display_name = EXCLUDED.display_name,
                        payment_data = EXCLUDED.payment_data,
                        is_active = EXCLUDED.is_active,
                        is_default = EXCLUDED.is_default
                    """);
                ps.setString(1, paymentMethod.paymentMethodId());
                ps.setString(2, paymentMethod.customerId());
                ps.setString(3, paymentMethod.paymentMethodType().name().toLowerCase());
                ps.setString(4, paymentMethod.displayName());
                ps.setString(5, paymentMethod.paymentData().toString());
                ps.setBoolean(6, paymentMethod.isActive());
                ps.setBoolean(7, paymentMethod.isDefault());
                return ps;
            });
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to upsert payment method to database: " + e.getMessage(), e);
        }
    }

    public void deletePaymentMethodsForCustomer(String customerId) {
        try {
            jdbcTemplate.update("DELETE FROM payment_method_ WHERE customer_id = ?", customerId);
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to delete payment methods for customer: " + e.getMessage(), e);
        }
    }

    public List<CustomerPaymentMethod> selectPaymentMethodsByCustomerId(String customerId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT 
                    payment_method_id, 
                    customer_id, 
                    payment_method_type, 
                    display_name, 
                    payment_data, 
                    is_active, 
                    is_default
                FROM payment_method_
                WHERE customer_id = ? AND is_active = true
                ORDER BY is_default DESC, created_at DESC
                """,
                (rs, rowNum) -> {
                    String paymentDataString = rs.getString("payment_data");
                    JsonNode paymentData;
                    try {
                        paymentData = objectMapper.readTree(paymentDataString);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse payment_data json", e);
                    }
                    
                    return new CustomerPaymentMethod(
                        rs.getString("payment_method_id"),
                        rs.getString("customer_id"),
                        PaymentMethodType.valueOf(rs.getString("payment_method_type").toUpperCase()),
                        rs.getString("display_name"),
                        paymentData,
                        rs.getBoolean("is_active"),
                        rs.getBoolean("is_default")
                    );
                },
                customerId
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to fetch payment methods for customer: " + customerId, e);
        }
    }
}
