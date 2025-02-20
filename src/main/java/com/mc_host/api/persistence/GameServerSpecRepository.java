package com.mc_host.api.persistence;

import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.Currency;

@Service
public class GameServerSpecRepository {

    private final JdbcTemplate jdbcTemplate;

    public GameServerSpecRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Fetch an equivalent price id, in a given currency
    public Optional<String> convertPrice(String priceId, Currency currency) {
        try {
            return jdbcTemplate.query(
                """
                SELECT plan2_.price_id
                FROM plan_ plan1_
                JOIN plan_ plan2_ ON plan1_.specification_id = plan2_.specification_id
                JOIN price_ ON plan2_.price_id = price_.price_id
                WHERE plan1_.price_id = ?
                AND price_.currency = ?
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
