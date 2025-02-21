package com.mc_host.api.persistence;

import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mc_host.api.model.game_server.GameServer;
import com.mc_host.api.model.game_server.ProvisioningState;

@Service
public class GameServerRepository {

    private final JdbcTemplate jdbcTemplate;

    public GameServerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int insertNewJavaServer(GameServer javaServer) {
        try {
            return jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement("""
                    INSERT INTO game_server_ (
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
            throw new RuntimeException("Failed to create new game server: " + e.getMessage(), e);
        }
    }

    public int updateJavaServer(GameServer javaServer) {
        try {
            int rowsAffected = jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement("""
                    UPDATE game_server_
                    SET subscription_id = ?,
                        plan_id = ?,
                        node_id = ?,
                        provisioning_state = ?,
                        retry_count = ?
                    WHERE server_id = ?
                    """);
                ps.setString(1, javaServer.getSubscriptionId());
                ps.setString(2, javaServer.getPlanId());
                ps.setString(3, javaServer.getNodeId());
                ps.setString(4, javaServer.getProvisioningState().name());
                ps.setInt(5, javaServer.getRetryCount());
                ps.setString(6, javaServer.getServerId());
                return ps;
            });
            
            if (rowsAffected == 0) {
                throw new RuntimeException("No existing game server found with id: " + javaServer.getServerId());
            }
            
            return rowsAffected;
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to update game server: " + e.getMessage(), e);
        }
    }

    public Optional<GameServer> selectJavaServerFromSubscription(String subscriptionId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT
                    server_id,
                    subscription_id,
                    plan_id,
                    node_id,
                    provisioning_state,
                    retry_count
                FROM game_server_
                WHERE subscription_id = ?
                """,
                (rs, rowNum) -> new GameServer(
                    rs.getString("server_id"), 
                    rs.getString("subscription_id"), 
                    rs.getString("plan_id"),
                    rs.getString("node_id"), 
                    ProvisioningState.valueOf(rs.getString("provisioning_state")),
                    rs.getInt("retry_count")),
                subscriptionId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to fetch java server for subscription id %s", subscriptionId), e);
        }
    }
}
