package com.mc_host.api.repository;

import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mc_host.api.model.game_server.DnsCNameRecord;
import com.mc_host.api.model.game_server.PterodactylServer;

@Service
public class GameServerRepository {

    private final JdbcTemplate jdbcTemplate;

    public GameServerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    // --- Pterodactyl Server operations ---
    
    public int insertPterodactylServer(PterodactylServer pterodactylServer) {
        try {
            return jdbcTemplate.update("""
                INSERT INTO pterodactyl_server_ (
                    subscription_id,
                    pterodactyl_server_uid,
                    pterodactyl_server_id,
                    allocation_id
                )
                VALUES (?, ?, ?, ?)
                """,
                pterodactylServer.subscriptionId(),
                pterodactylServer.pterodactylServerUid(),
                pterodactylServer.pterodactylServerId(),
                pterodactylServer.allocationId()
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to create pterodactyl server: " + e.getMessage(), e);
        }
    }
    
    public Optional<PterodactylServer> selectPterodactylServer(String subscriptionId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT
                    subscription_id,
                    pterodactyl_server_uid,
                    pterodactyl_server_id,
                    allocation_id
                FROM pterodactyl_server_
                WHERE subscription_id = ?
                """,
                (rs, rowNum) -> new PterodactylServer(
                    rs.getString("subscription_id"),
                    rs.getString("pterodactyl_server_uid"),
                    rs.getLong("pterodactyl_server_id"),
                    rs.getLong("allocation_id")),
                subscriptionId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to fetch pterodactyl server for subscription id %s", subscriptionId), e);
        }
    }
    
    public int deletePterodactylServer(String subscriptionId) {
        try {
            return jdbcTemplate.update("""
                DELETE FROM pterodactyl_server_
                WHERE subscription_id = ?
                """,
                subscriptionId
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to delete pterodactyl server: " + e.getMessage(), e);
        }
    }

    public int deletePterodactylServer(Long pterodactylServerId) {
        try {
            return jdbcTemplate.update("""
                DELETE FROM pterodactyl_server_
                WHERE pterodactyl_server_id = ?
                """,
                pterodactylServerId
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to delete pterodactyl server: " + e.getMessage(), e);
        }
    }
    
    // --- DNS C Record operations ---
    
    public int insertDnsCNameRecord(DnsCNameRecord dnsCNameRecord) {
        try {
            return jdbcTemplate.update("""
                INSERT INTO dns_c_name_record_ (
                    server_id,
                    c_name_record_id,
                    zone_id,
                    zone_name,
                    record_name,
                    content
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                dnsCNameRecord.serverId(),
                dnsCNameRecord.cNameRecordId(),
                dnsCNameRecord.zoneId(),
                dnsCNameRecord.zoneName(),
                dnsCNameRecord.recordName(),
                dnsCNameRecord.content()
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to create DNS C record: " + e.getMessage(), e);
        }
    }
    
    public Optional<DnsCNameRecord> selectDnsCNameRecord(String serverId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT
                    server_id,
                    c_name_record_id,
                    zone_id,
                    zone_name,
                    record_name,
                    content
                FROM dns_c_name_record_
                WHERE server_id = ?
                """,
                (rs, rowNum) -> new DnsCNameRecord(
                    rs.getString("server_id"),
                    rs.getString("c_name_record_id"),
                    rs.getString("zone_id"),
                    rs.getString("zone_name"),
                    rs.getString("record_name"),
                    rs.getString("content")),
                serverId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to fetch DNS C record for server id %s", serverId), e);
        }
    }
    
    public int deleteDnsCNameRecord(String serverId) {
        try {
            return jdbcTemplate.update("""
                DELETE FROM dns_c_name_record_
                WHERE c_name_record_id = ?
                """,
                serverId
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to delete DNS C record: " + e.getMessage(), e);
        }
    }
}