package com.mc_host.api.repository;

import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mc_host.api.model.node.PterodactylNode;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.Mode;
import com.mc_host.api.service.resources.v2.context.Status;
import com.mc_host.api.service.resources.v2.context.StepType;

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

    public Optional<Context> selectSubscription(String subscriptionId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT
                    subscription_id, 
                    step_type, 
                    mode,
                    execution_status
                FROM server_execution_context_
                WHERE subscription_id = ?
                """,
                (rs, rowNum) -> new Context(
                    rs.getString("subscription_id"), 
                    StepType.valueOf(rs.getString("step_type")), 
                    Mode.valueOf(rs.getString("mode")), 
                    Status.valueOf(rs.getString("execution_status"))
                ),
                subscriptionId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to fetch pterodactyl node for subscription id %s", subscriptionId), e);
        }
    }
}
