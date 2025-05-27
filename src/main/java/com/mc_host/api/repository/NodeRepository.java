package com.mc_host.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mc_host.api.model.hetzner.HetznerRegion;
import com.mc_host.api.model.node.HetznerNode;
import com.mc_host.api.model.node.PterodactylAllocation;
import com.mc_host.api.model.node.PterodactylNode;
import com.mc_host.api.model.node.DnsARecord;

@Service
public class NodeRepository {

    private final JdbcTemplate jdbcTemplate;

    public NodeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    // --- Hetzner Node operations ---
    public int insertHetznerCloudNode(HetznerNode hetznerNode) {
        try {
            return jdbcTemplate.update("""
                INSERT INTO cloud_node_ (
                    subscription_id,
                    node_id,
                    hetzner_region,
                    ipv4
                )
                VALUES (?, ?, ?, ?)
                """,
                hetznerNode.subscriptionId(),
                hetznerNode.nodeId(),
                hetznerNode.hetznerRegion().toString(),
                hetznerNode.ipv4()
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to create cloud node: " + e.getMessage(), e);
        }
    }
    
    public Optional<HetznerNode> selectHetznerNode(String subscriptionId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT
                    subscription_id,
                    node_id,
                    hetzner_region,
                    ipv4
                FROM cloud_node_
                WHERE subscription_id = ?
                """,
                (rs, rowNum) -> new HetznerNode(
                    rs.getString("subscription_id"),
                    rs.getLong("node_id"),
                    HetznerRegion.lookup(rs.getString("hetzner_region")),
                    rs.getString("ipv4")),
                    subscriptionId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to fetch cloud node for subscriptionId: %s", subscriptionId), e);
        }
    }

    public List<Long> selectAllHetznerNodeIds() {
        try {
            return jdbcTemplate.query(
                """
                SELECT
                    node_id
                FROM cloud_node_
                """,
                (rs, rowNum) -> rs.getLong("node_id")
            );
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to fetch all cloud nodes"), e);
        }
    }

    public int deleteHetznerNode(Long nodeId) {
        try {
            return jdbcTemplate.update("""
                DELETE FROM cloud_node_
                WHERE node_id = ?
                """,
                nodeId
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to delete cloud node: " + nodeId, e);
        }
    }
    
    // --- Pterodactyl Node operations ---
    
    public int insertPterodactylNode(PterodactylNode pterodactylNode) {
        try {
            return jdbcTemplate.update("""
                INSERT INTO pterodactyl_node_ (
                    subscription_id,
                    pterodactyl_node_id
                )
                VALUES (?, ?)
                """,
                pterodactylNode.subscriptionId(),
                pterodactylNode.pterodactylNodeId()
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to create pterodactyl node: " + e.getMessage(), e);
        }
    }
    
    public Optional<PterodactylNode> selectPterodactylNode(String subscriptionId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT
                    subscription_id,
                    pterodactyl_node_id
                FROM pterodactyl_node_
                WHERE subscription_id = ?
                """,
                (rs, rowNum) -> new PterodactylNode(
                    rs.getString("subscription_id"),
                    rs.getLong("pterodactyl_node_id")),
                    subscriptionId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to fetch pterodactyl node for subscription id %s", subscriptionId), e);
        }
    }
    
    public int deletePterodactylNodeFromNodeId(String subscriptionId) {
        try {
            return jdbcTemplate.update("""
                DELETE FROM pterodactyl_node_
                WHERE subscription_id = ?
                """,
                subscriptionId
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to delete pterodactyl node: " + e.getMessage(), e);
        }
    }

    public int deletePterodactylNode(Long pterodactylNodeId) {
        try {
            return jdbcTemplate.update("""
                DELETE FROM pterodactyl_node_
                WHERE pterodactyl_node_id = ?
                """,
                pterodactylNodeId
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to delete pterodactyl node: " + e.getMessage(), e);
        }
    }

    // --- Pterodactyl allocation operations ---

    public int insertPterodactylAllocation(PterodactylAllocation pterodactylAllocation) {
        try {
            return jdbcTemplate.update("""
                INSERT INTO pterodactyl_allocation_ (
                    subscription_id,
                    allocation_id,
                    ip,
                    port,
                    alias
                )
                VALUES (?, ?, ?, ?, ?)
                """,
                pterodactylAllocation.subscriptionId(),
                pterodactylAllocation.allocationId(),
                pterodactylAllocation.ip(),
                pterodactylAllocation.port(),
                pterodactylAllocation.alias()
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to insert pterodactyl allocation: " + pterodactylAllocation.allocationId(), e);
        }
    }

    public Optional<PterodactylAllocation> selectPterodactylAllocation(String subscriptionId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT
                    subscription_id,
                    allocation_id,
                    ip,
                    port,
                    alias
                FROM pterodactyl_allocation_
                WHERE subscription_id = ?
                """,
                (rs, rowNum) -> new PterodactylAllocation(
                    rs.getString("subscription_id"),
                    rs.getLong("allocation_id"),
                    rs.getString("ip"),
                    rs.getInt("port"),
                    rs.getString("alias")
                ),
                    subscriptionId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to fetch pterodactyl node for subscription id %s", subscriptionId), e);
        }
    }
    
    // --- DNS A Record operations ---
    
    public int insertDnsARecord(DnsARecord dnsARecord) {
        try {
            return jdbcTemplate.update("""
                INSERT INTO dns_a_record_ (
                    subscription_id,
                    a_record_id,
                    zone_id,
                    zone_name,
                    record_name,
                    content
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                dnsARecord.subscriptionId(),
                dnsARecord.aRecordId(),
                dnsARecord.zoneId(),
                dnsARecord.zoneName(),
                dnsARecord.recordName(),
                dnsARecord.content()
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to create DNS A record: " + e.getMessage(), e);
        }
    }
    
    public Optional<DnsARecord> selectDnsARecord(String subscriptionId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT
                    subscription_id,
                    a_record_id,
                    zone_id,
                    zone_name,
                    record_name,
                    content
                FROM dns_a_record_
                WHERE subscription_id = ?
                """,
                (rs, rowNum) -> new DnsARecord(
                    rs.getString("subscription_id"),
                    rs.getString("a_record_id"),
                    rs.getString("zone_id"),
                    rs.getString("zone_name"),
                    rs.getString("record_name"),
                    rs.getString("content")),
                    subscriptionId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to fetch DNS A record for subscription id %s", subscriptionId), e);
        }
    }

    public List<DnsARecord> selectAllARecordIds() {
        try {
            return jdbcTemplate.query(
                """
                SELECT
                    subscription_id,
                    a_record_id,
                    zone_id,
                    zone_name,
                    record_name,
                    content
                FROM dns_a_record_
                """,
                (rs, rowNum) -> new DnsARecord(
                    rs.getString("subscription_id"),
                    rs.getString("a_record_id"),
                    rs.getString("zone_id"),
                    rs.getString("zone_name"),
                    rs.getString("record_name"),
                    rs.getString("content"))
            );
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to fetch all a records"), e);
        }
    }
    
    public int deleteDnsARecord(String aRecordId) {
        try {
            return jdbcTemplate.update("""
                DELETE FROM dns_a_record_
                WHERE a_record_id = ?
                """,
                aRecordId
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to delete DNS A record: " + e.getMessage(), e);
        }
    }
}