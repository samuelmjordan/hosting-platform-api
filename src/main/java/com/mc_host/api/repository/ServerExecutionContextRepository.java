package com.mc_host.api.repository;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mc_host.api.service.resources.v2.context.Context;

@Service
public class ServerExecutionContextRepository {

    private final JdbcTemplate jdbcTemplate;

    public ServerExecutionContextRepository(
        JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsertSubscription(Context context) {
        try {
            jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement("""
                    INSERT INTO server_execution_context_ (
                        subscription_id, 
                        step_type, 
                        mode,
                        execution_status
                    )
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT (subscription_id) DO UPDATE SET
                        step_type = EXCLUDED.step_type,
                        mode = EXCLUDED.mode,
                        execution_status = EXCLUDED.execution_status
                    """);
                ps.setString(1, context.getSubscriptionId());
                ps.setString(2, context.getStepType().name());
                ps.setString(3, context.getMode().name());
                ps.setString(4, context.getStatus().name());
                return ps;
            });
        } catch (DataAccessException e) {
            throw new RuntimeException(
                "Failed to insert or update server execution context for subscription: " + context.getSubscriptionId(), e
            );
        }
    }
}
