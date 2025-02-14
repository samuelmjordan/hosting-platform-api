package com.mc_host.api.persistence;

import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.Currency;

@Service
public class JavaServerSpecPersistenceService {

    private final JdbcTemplate jdbcTemplate;

    public JavaServerSpecPersistenceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<String> validatePriceCurrency(String priceId, Currency currency) {
        try {
            return jdbcTemplate.query(
                """
                SELECT my_java_server_specification_ AS (
                    SELECT plan_.specification_id
                    FROM plan_
                    WHERE plan_.price_id = ?
                )
                SELECT plan_.price_id
                FROM plan_
                JOIN price_ ON price_.price_id = plan_.price_id
                JOIN my_java_server_specification_ ON my_java_server_specification_.specification_id = plan_.specification_id
                WHERE price_.currency = ?
                """,
                (rs, rowNum) -> rs.getString("price_id"),
                priceId,
                currency.name()
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to validate currency for price %s and currency %s", priceId, currency), e);
        }
    }
}
