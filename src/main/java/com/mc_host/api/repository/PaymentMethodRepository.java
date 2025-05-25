package com.mc_host.api.repository;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mc_host.api.model.entity.CustomerPaymentMethod;

@Service
public class PaymentMethodRepository {

    private final JdbcTemplate jdbcTemplate;

    public PaymentMethodRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
}
