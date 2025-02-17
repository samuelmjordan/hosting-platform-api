package com.mc_host.api.persistence;

import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mc_host.api.model.entity.server.JavaServer;
import com.mc_host.api.model.entity.server.ProvisioningState;

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
                        retry_count
                    )
                    VALUES (?, ?, ?, ?, ?)
                    """);
                ps.setString(1, javaServer.getServerId());
                ps.setString(2, javaServer.getSubscriptionId());
                ps.setString(3, javaServer.getPlanId());
                ps.setString(4, javaServer.getProvisioningState().name());
                ps.setInt(5, javaServer.getRetryCount());
                return ps;
            });
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to create new java server: " + e.getMessage(), e);
        }
    }

    public int updateJavaServer(JavaServer javaServer) {
        try {
            int rowsAffected = jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement("""
                    UPDATE java_server_
                    SET subscription_id = ?,
                        plan_id = ?,
                        hetzner_id = ?,
                        pterodactyl_id = ?,
                        provisioning_state = ?,
                        retry_count = ?
                    WHERE server_id = ?
                    """);
                ps.setString(1, javaServer.getSubscriptionId());
                ps.setString(2, javaServer.getPlanId());
                ps.setString(3, javaServer.getPlanId());
                ps.setString(4, javaServer.getPlanId());
                ps.setString(5, javaServer.getProvisioningState().name());
                ps.setInt(6, javaServer.getRetryCount());
                ps.setString(7, javaServer.getServerId());
                return ps;
            });
            
            if (rowsAffected == 0) {
                throw new RuntimeException("No existing java server found with id: " + javaServer.getServerId());
            }
            
            return rowsAffected;
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to update java server: " + e.getMessage(), e);
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
                    provisioning_state,
                    retry_count
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
                    rs.getInt("retry_count")),
                subscriptionId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to fetch java server for subscription id %s", subscriptionId), e);
        }
    }
}
