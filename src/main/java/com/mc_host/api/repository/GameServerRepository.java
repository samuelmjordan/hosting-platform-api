package com.mc_host.api.repository;

import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mc_host.api.model.resource.DnsCNameRecord;
import com.mc_host.api.model.resource.pterodactyl.PterodactylServer;

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
    
    public Optional<PterodactylServer> selectPterodactylServer(Long pterodactylServerId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT
                    subscription_id,
                    pterodactyl_server_uid,
                    pterodactyl_server_id,
                    allocation_id
                FROM pterodactyl_server_
                WHERE pterodactyl_server_id = ?
                """,
                (rs, rowNum) -> new PterodactylServer(
                    rs.getString("subscription_id"),
                    rs.getString("pterodactyl_server_uid"),
                    rs.getLong("pterodactyl_server_id"),
                    rs.getLong("allocation_id")),
                pterodactylServerId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to fetch pterodactyl server %s", pterodactylServerId), e);
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
                    subscription_id,
                    c_name_record_id,
                    zone_id,
                    zone_name,
                    record_name,
                    content
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                dnsCNameRecord.subscriptionId(),
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

    public int updateDnsCNameRecord(DnsCNameRecord dnsCNameRecord) {
        try {
            return jdbcTemplate.update("""
                UPDATE dns_c_name_record_ SET
                    subscription_id = ?,
                    zone_id = ?,
                    zone_name = ?,
                    record_name = ?,
                    content = ?
                WHERE c_name_record_id = ?
                """,
                dnsCNameRecord.subscriptionId(),
                dnsCNameRecord.zoneId(),
                dnsCNameRecord.zoneName(),
                dnsCNameRecord.recordName(),
                dnsCNameRecord.content(),
                dnsCNameRecord.cNameRecordId()
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to update DNS C record: " + e.getMessage(), e);
        }
    }
    
    public Optional<DnsCNameRecord> selectDnsCNameRecord(String cNameRecordId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT
                    subscription_id,
                    c_name_record_id,
                    zone_id,
                    zone_name,
                    record_name,
                    content
                FROM dns_c_name_record_
                WHERE c_name_record_id = ?
                """,
                (rs, rowNum) -> new DnsCNameRecord(
                    rs.getString("subscription_id"),
                    rs.getString("c_name_record_id"),
                    rs.getString("zone_id"),
                    rs.getString("zone_name"),
                    rs.getString("record_name"),
                    rs.getString("content")),
                cNameRecordId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to fetch DNS C record %s", cNameRecordId), e);
        }
    }

    public boolean isDnsCNameRecordNameTaken(String recordName, String zoneId) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM dns_c_name_record_
                WHERE record_name = ? AND zone_id = ?
                """,
                Integer.class,
                recordName,
                zoneId
            );
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("failed to check if record name %s is taken in zone %s", recordName, zoneId), e);
        }
    }

    public Optional<DnsCNameRecord> selectDnsCNameRecordWithSubscriptionId(String subscriptionId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT
                    subscription_id,
                    c_name_record_id,
                    zone_id,
                    zone_name,
                    record_name,
                    content
                FROM dns_c_name_record_
                WHERE subscription_id = ?
                ORDER BY created_at ASC
                LIMIT 1
                """,
                (rs, rowNum) -> new DnsCNameRecord(
                    rs.getString("subscription_id"),
                    rs.getString("c_name_record_id"),
                    rs.getString("zone_id"),
                    rs.getString("zone_name"),
                    rs.getString("record_name"),
                    rs.getString("content")),
                subscriptionId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to fetch DNS C record %s", subscriptionId), e);
        }
    }
    
    public int deleteDnsCNameRecord(String cnameRecordId) {
        try {
            return jdbcTemplate.update("""
                DELETE FROM dns_c_name_record_
                WHERE c_name_record_id = ?
                """,
                cnameRecordId
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to delete DNS C record: " + e.getMessage(), e);
        }
    }
}