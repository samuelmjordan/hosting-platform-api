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
            return jdbcTemplate.update("""
                INSERT INTO game_server_ (
                    server_id, 
                    subscription_id,
                    plan_id,
                    node_id
                )
                VALUES (?, ?, ?, ?)
                """,
                javaServer.getServerId(),
                javaServer.getSubscriptionId(),
                javaServer.getPlanId(),
                javaServer.getNodeId()
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to create new game server: " + e.getMessage(), e);
        }
    }

    public int updateJavaServer(GameServer javaServer) {
        try {
            int rowsAffected = jdbcTemplate.update("""
                UPDATE game_server_
                SET subscription_id = ?,
                    plan_id = ?,
                    pterodactyl_server_id = ?,
                    node_id = ?,
                    allocation_id = ?,
                    port = ?,
                    c_name_record_id = ?,
                    zone_name = ?,
                    record_name  = ?
                WHERE server_id = ?
                """,
                javaServer.getSubscriptionId(),
                javaServer.getPlanId(),
                javaServer.getPterodactylServerId(),
                javaServer.getNodeId(),
                javaServer.getAllocationId(),
                javaServer.getPort(),
                javaServer.getCNameRecordId(),
                javaServer.getZoneName(),
                javaServer.getRecordName(),
                javaServer.getServerId()
            );
            if (rowsAffected == 0) {
                throw new RuntimeException("No existing game server found with id: " + javaServer.getServerId());
            }
            
            return rowsAffected;
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to update game server: " + e.getMessage(), e);
        }
    }

    public Optional<GameServer> selectGameServerFromSubscription(String subscriptionId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT
                    server_id,
                    subscription_id,
                    plan_id,
                    pterodactyl_server_id,
                    node_id,
                    allocation_id,
                    port,
                    c_name_record_id,
                    zone_name,
                    record_name
                FROM game_server_
                WHERE subscription_id = ?
                """,
                (rs, rowNum) -> new GameServer(
                    rs.getString("server_id"), 
                    rs.getString("subscription_id"), 
                    rs.getString("plan_id"),
                    rs.getString("node_id"), 
                    rs.getLong("pterodactyl_server_id"),
                    rs.getLong("allocation_id"),
                    rs.getInt("port"),
                    rs.getString("c_name_record_id"),
                    rs.getString("zone_name"),
                    rs.getString("record_name")),
                subscriptionId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to fetch java server for subscription id %s", subscriptionId), e);
        }
    }
}
