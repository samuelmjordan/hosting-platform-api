package com.mc_host.api.repository;

import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mc_host.api.model.MarketingRegion;
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
                        execution_status,
                        region,
                        specification_id,
                        title,
                        caption,
                        node_id,
                        a_record_id,
                        pterodactyl_node_id,
                        allocation_id,
                        pterodactyl_server_id,
                        c_name_record_id,
                        new_node_id,
                        new_a_record_id,
                        new_pterodactyl_node_id,
                        new_allocation_id,
                        new_pterodactyl_server_id,
                        new_c_name_record_id
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (subscription_id) DO UPDATE SET
                        step_type = EXCLUDED.step_type,
                        mode = EXCLUDED.mode,
                        execution_status = EXCLUDED.execution_status,
                        region = EXCLUDED.region,
                        specification_id = EXCLUDED.specification_id,
                        title = EXCLUDED.title,
                        caption = EXCLUDED.caption,
                        node_id = EXCLUDED.node_id,
                        a_record_id = EXCLUDED.a_record_id,
                        pterodactyl_node_id = EXCLUDED.pterodactyl_node_id,
                        allocation_id = EXCLUDED.allocation_id,
                        pterodactyl_server_id = EXCLUDED.pterodactyl_server_id,
                        c_name_record_id = EXCLUDED.c_name_record_id,
                        new_node_id = EXCLUDED.new_node_id,
                        new_a_record_id = EXCLUDED.new_a_record_id,
                        new_pterodactyl_node_id = EXCLUDED.new_pterodactyl_node_id,
                        new_allocation_id = EXCLUDED.new_allocation_id,
                        new_pterodactyl_server_id = EXCLUDED.new_pterodactyl_server_id,
                        new_c_name_record_id = EXCLUDED.new_c_name_record_id
                    """);
                ps.setString(1, context.getSubscriptionId());
                ps.setString(2, context.getStepType().name());
                ps.setString(3, context.getMode().name());
                ps.setString(4, context.getStatus().name());
                ps.setString(5, context.getRegion().name());
                ps.setString(6, context.getSpecificationId());
                ps.setString(7, context.getTitle());
                ps.setString(8, context.getCaption());
                ps.setObject(9, context.getNodeId());
                ps.setString(10, context.getARecordId());
                ps.setObject(11, context.getPterodactylNodeId());
                ps.setObject(12, context.getAllocationId());
                ps.setObject(13, context.getPterodactylServerId());
                ps.setObject(14, context.getCNameRecordId());
                ps.setObject(15, context.getNewNodeId());
                ps.setString(16, context.getNewARecordId());
                ps.setObject(17, context.getNewPterodactylNodeId());
                ps.setObject(18, context.getNewAllocationId());
                ps.setObject(19, context.getNewPterodactylServerId());
                ps.setObject(20, context.getNewCNameRecordId());
                return ps;
            });
        } catch (DataAccessException e) {
            throw new RuntimeException(
                String.format("Failed to insert or update server execution context for subscription: %s, context:", context.getSubscriptionId(), context.toString()), e
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
                    caption,
                    node_id,
                    a_record_id,
                    pterodactyl_node_id,
                    allocation_id,
                    pterodactyl_server_id,
                    c_name_record_id,
                    new_node_id,
                    new_a_record_id,
                    new_pterodactyl_node_id,
                    new_allocation_id,
                    new_pterodactyl_server_id,
                    new_c_name_record_id
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
                    rs.getString("caption"),
                    (Long) rs.getObject("node_id"),
                    rs.getString("a_record_id"),
                    (Long) rs.getObject("pterodactyl_node_id"),
                    (Long) rs.getObject("allocation_id"),
                    (Long) rs.getObject("pterodactyl_server_id"),
                    rs.getString("c_name_record_id"),
                    (Long) rs.getObject("new_node_id"),
                    rs.getString("new_a_record_id"),
                    (Long) rs.getObject("new_pterodactyl_node_id"),
                    (Long) rs.getObject("new_allocation_id"),
                    (Long) rs.getObject("new_pterodactyl_server_id"),
                    rs.getString("new_c_name_record_id")
                ),
                subscriptionId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to fetch server execution context for subscription id %s", subscriptionId), e);
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
}
