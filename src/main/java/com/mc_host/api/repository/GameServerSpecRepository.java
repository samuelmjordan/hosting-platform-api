package com.mc_host.api.repository;

import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.AcceptedCurrency;
import com.mc_host.api.model.specification.JavaServerSpecification;

@Service
public class GameServerSpecRepository {

    private final JdbcTemplate jdbcTemplate;

    public GameServerSpecRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Fetch an equivalent price id, in a given currency
    public Optional<String> convertPrice(String priceId, AcceptedCurrency currency) {
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

    public Optional<JavaServerSpecification> selectSpecification(String specification_id) {
        try {
            return jdbcTemplate.query(
                """
                SELECT 
                    specification_id,
                    title,
                    caption,
                    ram_gb,
                    vcpu,
                    ssd_gb
                FROM game_server_specification_
                WHERE specification_id = ?
                """,
                (rs, rowNum) -> new JavaServerSpecification(
                    rs.getString("specification_id"),
                    rs.getString("title"),
                    rs.getString("caption"),
                    rs.getString("ram_gb"),
                    rs.getString("vcpu"),
                    rs.getString("ssd_gb")
                ),
                specification_id
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to fetch specification", e);
        }
    }
}
