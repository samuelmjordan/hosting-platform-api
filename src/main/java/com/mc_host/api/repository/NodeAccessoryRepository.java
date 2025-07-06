package com.mc_host.api.repository;

import com.mc_host.api.model.resource.dns.DnsARecord;
import com.mc_host.api.model.resource.pterodactyl.PterodactylAllocation;
import com.mc_host.api.model.resource.pterodactyl.PterodactylNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Service
public class NodeAccessoryRepository extends BaseRepository {

    public NodeAccessoryRepository(JdbcTemplate jdbc) { super(jdbc); }

    // pterodactyl node ops
    public int insertPterodactylNode(PterodactylNode node) {
        return execute("INSERT INTO pterodactyl_node_ (subscription_id, pterodactyl_node_id) VALUES (?, ?)",
                node.subscriptionId(), node.pterodactylNodeId());
    }

    public Optional<PterodactylNode> selectPterodactylNode(Long nodeId) {
        return selectOne("""
            SELECT
                subscription_id,
                pterodactyl_node_id
            FROM pterodactyl_node_
            WHERE pterodactyl_node_id = ?
            """,
            this::mapPterodactylNode,
            nodeId
        );
    }

    public int deletePterodactylNode(String subId) {
        return execute("DELETE FROM pterodactyl_node_ WHERE subscription_id = ?", subId);
    }

    public int deletePterodactylNode(Long nodeId) {
        return execute("DELETE FROM pterodactyl_node_ WHERE pterodactyl_node_id = ?", nodeId);
    }

    // pterodactyl allocation ops
    public int insertPterodactylAllocation(PterodactylAllocation allocation) {
        return execute("""
            INSERT INTO pterodactyl_allocation_ (
                subscription_id,
                allocation_id,
                ip,
                port,
                alias)
            VALUES (?, ?, ?, ?, ?)
            """,
            allocation.subscriptionId(),
            allocation.allocationId(),
            allocation.ip(),
            allocation.port(),
            allocation.alias()
        );
    }

    public Optional<PterodactylAllocation> selectPterodactylAllocation(Long allocId) {
        return selectOne("""
            SELECT
                subscription_id,
                allocation_id,
                ip,
                port,
                alias
            FROM pterodactyl_allocation_
            WHERE allocation_id = ?
            """,
            this::mapPterodactylAllocation,
            allocId
        );
    }

    public int deletePterodactylAllocation(Long allocId) {
        return execute("DELETE FROM pterodactyl_allocation_ WHERE allocation_id = ?", allocId);
    }

    // dns a record ops
    public int insertDnsARecord(DnsARecord record) {
        return execute("""
            INSERT INTO dns_a_record_ (
                subscription_id,
                a_record_id,
                zone_id,
                zone_name,
                record_name,
                content)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            record.subscriptionId(),
            record.aRecordId(),
            record.zoneId(),
            record.zoneName(),
            record.recordName(),
            record.content()
        );
    }

    public Optional<DnsARecord> selectDnsARecord(String recordId) {
        return selectOne("""
            SELECT
                subscription_id,
                a_record_id,
                zone_id,
                zone_name,
                record_name,
                content
            FROM dns_a_record_
            WHERE a_record_id = ?
            """,
            this::mapDnsARecord,
            recordId
        );
    }

    public List<DnsARecord> selectAllARecordIds() {
        return selectMany("""
            SELECT
                subscription_id,
                a_record_id,
                zone_id,
                zone_name,
                record_name,
                content
            FROM dns_a_record_
            """,
            this::mapDnsARecord
        );
    }

    public int deleteDnsARecord(String recordId) {
        return execute("DELETE FROM dns_a_record_ WHERE a_record_id = ?", recordId);
    }

    // mappers
    private PterodactylNode mapPterodactylNode(ResultSet rs, int rowNum) throws SQLException {
        return new PterodactylNode(
            rs.getString("subscription_id"),
            rs.getLong("pterodactyl_node_id"));
    }

    private PterodactylAllocation mapPterodactylAllocation(ResultSet rs, int rowNum) throws SQLException {
        return new PterodactylAllocation(
            rs.getString("subscription_id"),
            rs.getLong("allocation_id"),
            rs.getString("ip"),
            rs.getInt("port"),
            rs.getString("alias"));
    }

    private DnsARecord mapDnsARecord(ResultSet rs, int rowNum) throws SQLException {
        return new DnsARecord(
            rs.getString("subscription_id"),
            rs.getString("a_record_id"),
            rs.getString("zone_id"),
            rs.getString("zone_name"),
            rs.getString("record_name"),
            rs.getString("content"));
    }
}