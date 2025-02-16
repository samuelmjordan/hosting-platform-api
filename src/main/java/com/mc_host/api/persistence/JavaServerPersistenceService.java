package com.mc_host.api.persistence;

import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mc_host.api.model.entity.JavaServerEntity;

@Service
public class JavaServerPersistenceService {

    private final JdbcTemplate jdbcTemplate;

    public JavaServerPersistenceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<JavaServerEntity> selectJavaServerFromSubscription(String subscription_id) {
        try {
            return Optional.empty();
            /* return jdbcTemplate.query(
                """
                SELECT
                    server_id,
                    hetzner_id,
                    pterodactyl_id,
                    subscription_id,
                    plan_id
                FROM java_server_
                WHERE subscription_id = ?
                """,
                (rs, rowNum) -> new JavaServerEntity(
                    rs.getString("server_id"), 
                    rs.getString("hetzner_id"), 
                    rs.getString("pterodactyl_id"), 
                    rs.getString("subscription_id"), 
                    rs.getString("plan_id")),
                subscription_id
            ).stream().findFirst(); */
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to fetch java server for subscription id %s", subscription_id), e);
        }
    }
}
