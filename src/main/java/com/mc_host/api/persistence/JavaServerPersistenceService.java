package com.mc_host.api.persistence;

import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mc_host.api.model.entity.server.JavaServer;
import com.mc_host.api.model.entity.server.ProvisioningState;
import com.mc_host.api.model.entity.server.ProvisioningStatus;

@Service
public class JavaServerPersistenceService {

    private final JdbcTemplate jdbcTemplate;

    public JavaServerPersistenceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int insertNewJavaServer(JavaServer javaServer) {
        try {
            return jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement("""
                    INSERT INTO java_server_ (
                        server_id, 
                        subscription_id,
                        plan_id,
                        provisioning_state,
                        provisioning_status
                    )
                    VALUES (?, ?, ?, ?, ?)
                    """);
                ps.setString(1, javaServer.getServerId());
                ps.setString(2, javaServer.getSubscriptionId());
                ps.setString(3, javaServer.getPlanId());
                ps.setString(4, javaServer.getProvisioningState().name());
                ps.setString(5, javaServer.getProvisioningStatus().name());
                return ps;
            });
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to create new java server: " + e.getMessage(), e);
        }
    }

    public Optional<JavaServer> selectJavaServerFromSubscription(String subscriptionId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT
                    server_id,
                    hetzner_id,
                    pterodactyl_id,
                    subscription_id,
                    plan_id,
                    provisioning_state
                    provisioning_status
                FROM java_server_
                WHERE subscription_id = ?
                """,
                (rs, rowNum) -> new JavaServer(
                    rs.getString("server_id"), 
                    rs.getString("hetzner_id"), 
                    rs.getString("pterodactyl_id"), 
                    rs.getString("subscription_id"), 
                    rs.getString("plan_id"),
                    ProvisioningState.valueOf(rs.getString("provisioning_state")),
                    ProvisioningStatus.valueOf(rs.getString("provisioning_status"))),
                subscriptionId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to fetch java server for subscription id %s", subscriptionId), e);
        }
    }

    public int updateJavaServer(String serverId, ProvisioningState provisioningState) {
        try {
            return jdbcTemplate.update(
                """
                UPDATE java_server_
                SET provisioning_state = ?
                WHERE server_id = ?
                """,
                provisioningState.name(),
                serverId
            );
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to update java server provisioning status %s", serverId), e);
        }
    }
}
