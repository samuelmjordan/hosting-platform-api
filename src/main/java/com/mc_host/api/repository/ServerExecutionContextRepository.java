package com.mc_host.api.repository;

import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mc_host.api.model.MarketingRegion;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.ContextField;
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
                    execution_status,
                    region,
                    specification_id,
                    title,
                    caption
                FROM server_execution_context_
                WHERE subscription_id = ?
                """,
                (rs, rowNum) -> new Context(
                    rs.getString("subscription_id"), 
                    StepType.valueOf(rs.getString("step_type")), 
                    Mode.valueOf(rs.getString("mode")), 
                    Status.valueOf(rs.getString("execution_status")),
                    MarketingRegion.valueOf(rs.getString("region")),
                    rs.getString("specification_id"),
                    rs.getString("title"),
                    rs.getString("caption")
                ),
                subscriptionId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to fetch pterodactyl node for subscription id %s", subscriptionId), e);
        }
    }

    public void updateTitle(String subscriptionId, String title) {
        try {
            jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement("""
                    UPDATE server_execution_context_ 
                    SET title = ?
                    WHERE subscription_id = ?;
                    """);
                ps.setString(1, title);
                ps.setString(2, subscriptionId);
                return ps;
            });
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to update subscription title to database: " + e.getMessage(), e);
        }
    }

    public void updateCaption(String subscriptionId, String caption) {
        try {
            jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement("""
                    UPDATE server_execution_context_ 
                    SET caption = ?
                    WHERE subscription_id = ?;
                    """);
                ps.setString(1, caption);
                ps.setString(2, subscriptionId);
                return ps;
            });
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to update subscription title to database: " + e.getMessage(), e);
        }
    }

    public void updateRegion(String subscriptionId, MarketingRegion region) {
        try {
            jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement("""
                    UPDATE server_execution_context_ 
                    SET region = ?
                    WHERE subscription_id = ?;
                    """);
                ps.setString(1, region.name());
                ps.setString(2, subscriptionId);
                return ps;
            });
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to update subscription title to database: " + e.getMessage(), e);
        }
    }

    public void updateSpecification(String subscriptionId, String specificationId) {
        try {
            jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement("""
                    UPDATE server_execution_context_ 
                    SET specification_id = ?
                    WHERE subscription_id = ?;
                    """);
                ps.setString(1, specificationId);
                ps.setString(2, subscriptionId);
                return ps;
            });
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to update subscription title to database: " + e.getMessage(), e);
        }
    }

    public void promoteNewResourcesToCurrent(String subscriptionId) {
        try {
            jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement("""
                    UPDATE server_execution_context_
                    SET 
                        node_id = new_node_id,
                        a_record_id = new_a_record_id,
                        pterodactyl_node_id = new_pterodactyl_node_id,
                        allocation_id = new_allocation_id,
                        pterodactyl_server_id = new_pterodactyl_server_id,
                        c_name_record_id = new_c_name_record_id,
                        new_node_id = NULL,
                        new_a_record_id = NULL,
                        new_pterodactyl_node_id = NULL,
                        new_allocation_id = NULL,
                        new_pterodactyl_server_id = NULL,
                        new_c_name_record_id = NULL
                    WHERE subscription_id = ?;
                    """);
                ps.setString(1, subscriptionId);
                return ps;
            });
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to promote new resources to current for subscription: " + subscriptionId, e);
        }
    }

    private void updateField(String subscriptionId, ContextField field, Object value) {
        try {
            jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement(String.format("""
                    UPDATE server_execution_context_ 
                    SET %s = ?
                    WHERE subscription_id = ?;
                    """, field.getColumnName()));
                
                if (value instanceof Long) {
                    ps.setLong(1, (Long) value);
                } else if (value instanceof String) {
                    ps.setString(1, (String) value);
                } else if (value == null) {
                    ps.setNull(1, java.sql.Types.NULL);
                } else {
                    throw new IllegalArgumentException("Unsupported type: " + value.getClass());
                }
                
                ps.setString(2, subscriptionId);
                return ps;
            });
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to update context " + field.getColumnName() + " to database for subscription: " + subscriptionId, e);
        }
    }

    // Existing resource updates
    public void updateNodeId(String subscriptionId, Long nodeId) {
        updateField(subscriptionId, ContextField.NODE_ID, nodeId);
    }

    public void updateARecordId(String subscriptionId, String aRecordId) {
        updateField(subscriptionId, ContextField.A_RECORD_ID, aRecordId);
    }

    public void updatePterodactylNodeId(String subscriptionId, Long pterodactylNodeId) {
        updateField(subscriptionId, ContextField.PTERODACTYL_NODE_ID, pterodactylNodeId);
    }

    public void updateAllocationId(String subscriptionId, Long allocationId) {
        updateField(subscriptionId, ContextField.ALLOCATION_ID, allocationId);
    }

    public void updatePterodactylServerId(String subscriptionId, Long pterodactylServerId) {
        updateField(subscriptionId, ContextField.PTERODACTYL_SERVER_ID, pterodactylServerId);
    }

    public void updateCNameRecordId(String subscriptionId, String cNameRecordId) {
        updateField(subscriptionId, ContextField.C_NAME_RECORD_ID, cNameRecordId);
    }

    // New resource updates (migration targets)
    public void updateNewNodeId(String subscriptionId, Long newNodeId) {
        updateField(subscriptionId, ContextField.NEW_NODE_ID, newNodeId);
    }

    public void updateNewARecordId(String subscriptionId, String newARecordId) {
        updateField(subscriptionId, ContextField.NEW_A_RECORD_ID, newARecordId);
    }

    public void updateNewPterodactylNodeId(String subscriptionId, Long newPterodactylNodeId) {
        updateField(subscriptionId, ContextField.NEW_PTERODACTYL_NODE_ID, newPterodactylNodeId);
    }

    public void updateNewAllocationId(String subscriptionId, Long newAllocationId) {
        updateField(subscriptionId, ContextField.NEW_ALLOCATION_ID, newAllocationId);
    }

    public void updateNewPterodactylServerId(String subscriptionId, Long newPterodactylServerId) {
        updateField(subscriptionId, ContextField.NEW_PTERODACTYL_SERVER_ID, newPterodactylServerId);
    }

    public void updateNewCNameRecordId(String subscriptionId, String newCNameRecordId) {
        updateField(subscriptionId, ContextField.NEW_C_NAME_RECORD_ID, newCNameRecordId);
    }
}
