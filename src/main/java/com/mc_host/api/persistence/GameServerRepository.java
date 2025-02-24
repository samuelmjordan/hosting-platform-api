package com.mc_host.api.persistence;

import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mc_host.api.model.game_server.GameServer;

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
                        node_id,
                        subdomain
                    )
                    VALUES (?, ?, ?, ?, ?)
                    """);
                ps.setString(1, javaServer.getServerId());
                ps.setString(2, javaServer.getSubscriptionId());
                ps.setString(3, javaServer.getPlanId());
                ps.setString(4, javaServer.getNodeId());
                ps.setString(5, javaServer.getSubdomain());
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
                        subdomain = ?,
                    WHERE server_id = ?
                    """);
                ps.setString(1, javaServer.getSubscriptionId());
                ps.setString(2, javaServer.getPlanId());
                ps.setString(3, javaServer.getNodeId());
                ps.setString(4, javaServer.getSubdomain());
                ps.setString(5, javaServer.getServerId());
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
                    subdomain
                FROM game_server_
                WHERE subscription_id = ?
                """,
                (rs, rowNum) -> new GameServer(
                    rs.getString("server_id"), 
                    rs.getString("subscription_id"), 
                    rs.getString("plan_id"),
                    rs.getString("node_id"), 
                    rs.getString("subdomain")),
                subscriptionId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to fetch java server for subscription id %s", subscriptionId), e);
        }
    }
}
