package com.mc_host.api.repository;

import com.mc_host.api.model.resource.dns.DnsCNameRecord;
import com.mc_host.api.model.resource.pterodactyl.PterodactylServer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Service
public class GameServerRepository extends BaseRepository {

    public GameServerRepository(JdbcTemplate jdbc) { super(jdbc); }

    // Pterodactyl Server
    public int insertPterodactylServer(PterodactylServer server) {
        return execute("""
            INSERT INTO pterodactyl_server_ (
                subscription_id,
                pterodactyl_server_uid,
                pterodactyl_server_id,
                allocation_id,
                server_key
            )
            VALUES (?, ?, ?, ?, ?)
            """,
            server.subscriptionId(),
            server.pterodactylServerUid(),
            server.pterodactylServerId(),
            server.allocationId(),
            server.serverKey()
        );
    }

    public Optional<PterodactylServer> selectPterodactylServer(Long id) {
        return selectOne("""
            SELECT 
                subscription_id,
                pterodactyl_server_uid,
                pterodactyl_server_id,
                allocation_id,
                server_key
            FROM pterodactyl_server_
            WHERE pterodactyl_server_id = ?
            """,
            this::mapPterodactylServer,
            id
        );
    }

    public Boolean domainExists(String subdomain) {
        return selectOne("SELECT 1 FROM dns_c_name_record_ WHERE concat(?, '.', zone_name) = record_name",
            (rs, rowNum) -> true, subdomain)
            .isPresent();
    }

    public int deletePterodactylServer(Long serverId) {
        return execute("DELETE FROM pterodactyl_server_ WHERE pterodactyl_server_id = ?", serverId);
    }

    // C-Name Records
    public int insertDnsCNameRecord(DnsCNameRecord record) {
        return execute("""
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
            record.subscriptionId(),
            record.cNameRecordId(),
            record.zoneId(),
            record.zoneName(),
            record.recordName(),
            record.content()
        );
    }

    public int updateDnsCNameRecord(DnsCNameRecord record) {
        return execute("""
            UPDATE dns_c_name_record_ SET
                subscription_id = ?,
                zone_id = ?,
                zone_name = ?,
                record_name = ?,
                content = ?
            WHERE c_name_record_id = ?
            """,
            record.subscriptionId(),
            record.zoneId(),
            record.zoneName(),
            record.recordName(),
            record.content(),
            record.cNameRecordId()
        );
    }

    public Optional<DnsCNameRecord> selectDnsCNameRecord(String id) {
        return selectOne("""
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
            this::mapDnsCNameRecord,
            id
        );
    }

    public boolean isDnsCNameRecordNameTaken(String recordName, String zoneId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM dns_c_name_record_ WHERE record_name = ? AND zone_id = ?",
                Integer.class, recordName, zoneId);
        return count != null && count > 0;
    }

    public Optional<DnsCNameRecord> selectDnsCNameRecordWithSubscriptionId(String subId) {
        return selectOne("""
            SELECT
                subscription_id,
                c_name_record_id,
                zone_id, zone_name,
                record_name,
                content
            FROM dns_c_name_record_ WHERE subscription_id = ?
            ORDER BY created_at ASC LIMIT 1
            """,
            this::mapDnsCNameRecord,
            subId
        );
    }

    public int deleteDnsCNameRecord(String id) {
        return execute("DELETE FROM dns_c_name_record_ WHERE c_name_record_id = ?", id);
    }

    // Mappers
    private PterodactylServer mapPterodactylServer(ResultSet rs, int rowNum) throws SQLException {
        return new PterodactylServer(
                rs.getString("subscription_id"),
                rs.getString("pterodactyl_server_uid"),
                rs.getLong("pterodactyl_server_id"),
                rs.getLong("allocation_id"),
                rs.getString("server_key"));
    }

    private DnsCNameRecord mapDnsCNameRecord(ResultSet rs, int rowNum) throws SQLException {
        return new DnsCNameRecord(
                rs.getString("subscription_id"),
                rs.getString("c_name_record_id"),
                rs.getString("zone_id"),
                rs.getString("zone_name"),
                rs.getString("record_name"),
                rs.getString("content"));
    }
}