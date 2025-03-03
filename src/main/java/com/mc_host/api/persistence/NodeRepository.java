package com.mc_host.api.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mc_host.api.model.hetzner.HetznerRegion;
import com.mc_host.api.model.node.Node;
import com.mc_host.api.model.node.HetznerNode;
import com.mc_host.api.model.node.PterodactylNode;
import com.mc_host.api.model.node.DnsARecord;

@Service
public class NodeRepository {

    private final JdbcTemplate jdbcTemplate;

    public NodeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // --- Core Node operations ---

    public int insertNode(Node node) {
        try {
            return jdbcTemplate.update("""
                INSERT INTO node_ (
                    node_id,
                    dedicated
                )
                VALUES (?, ?)
                """,
                node.nodeId(),
                node.dedicated()
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to create node: " + e.getMessage(), e);
        }
    }

    public Optional<Node> selectNode(String nodeId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT
                    node_id,
                    dedicated
                FROM node_
                WHERE node_id = ?
                """,
                (rs, rowNum) -> new Node(
                    rs.getString("node_id"), 
                    rs.getBoolean("dedicated")),
                    nodeId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to fetch node for node id %s", nodeId), e);
        }
    }

    public int deleteNode(String nodeId) {
        try {
            return jdbcTemplate.update("""
                DELETE FROM node_
                WHERE node_id = ?
                """,
                nodeId
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to delete node: " + e.getMessage(), e);
        }
    }
    
    // --- Hetzner Node operations ---
    
    public int insertHetznerNode(HetznerNode hetznerNode) {
        try {
            String hetznerRegion = hetznerNode.hetznerRegion() == null ? null : hetznerNode.hetznerRegion().toString();
            return jdbcTemplate.update("""
                INSERT INTO hetzner_node_ (
                    node_id,
                    hetzner_node_id,
                    hetzner_region,
                    ipv4
                )
                VALUES (?, ?, ?, ?)
                """,
                hetznerNode.nodeId(),
                hetznerNode.hetznerNodeId(),
                hetznerRegion,
                hetznerNode.ipv4()
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to create hetzner node: " + e.getMessage(), e);
        }
    }
    
    public Optional<HetznerNode> selectHetznerNode(String nodeId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT
                    node_id,
                    hetzner_node_id,
                    hetzner_region,
                    ipv4
                FROM hetzner_node_
                WHERE node_id = ?
                """,
                (rs, rowNum) -> new HetznerNode(
                    rs.getString("node_id"),
                    rs.getLong("hetzner_node_id"),
                    HetznerRegion.lookup(rs.getString("hetzner_region")),
                    rs.getString("ipv4")),
                    nodeId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to fetch hetzner node for node id %s", nodeId), e);
        }
    }

    public List<Long> selectAllHetznerNodeIds() {
        try {
            return jdbcTemplate.query(
                """
                SELECT
                    hetzner_node_id
                FROM hetzner_node_
                """,
                (rs, rowNum) -> rs.getLong("hetzner_node_id")
            );
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to fetch all hetzner nodes"), e);
        }
    }
    
    public int deleteHetznerNodewithNodeId(String nodeId) {
        try {
            return jdbcTemplate.update("""
                DELETE FROM hetzner_node_
                WHERE node_id = ?
                """,
                nodeId
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to delete hetzner node: " + e.getMessage(), e);
        }
    }

    public int deleteHetznerNode(Long hetznerNodeId) {
        try {
            return jdbcTemplate.update("""
                DELETE FROM hetzner_node_
                WHERE hetzner_node_id = ?
                """,
                hetznerNodeId
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to delete hetzner node: " + e.getMessage(), e);
        }
    }
    
    // --- Pterodactyl Node operations ---
    
    public int insertPterodactylNode(PterodactylNode pterodactylNode) {
        try {
            return jdbcTemplate.update("""
                INSERT INTO pterodactyl_node_ (
                    node_id,
                    pterodactyl_node_id
                )
                VALUES (?, ?)
                """,
                pterodactylNode.nodeId(),
                pterodactylNode.pterodactylNodeId()
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to create pterodactyl node: " + e.getMessage(), e);
        }
    }
    
    public Optional<PterodactylNode> selectPterodactylNode(String nodeId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT
                    node_id,
                    pterodactyl_node_id
                FROM pterodactyl_node_
                WHERE node_id = ?
                """,
                (rs, rowNum) -> new PterodactylNode(
                    rs.getString("node_id"),
                    rs.getLong("pterodactyl_node_id")),
                    nodeId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to fetch pterodactyl node for node id %s", nodeId), e);
        }
    }
    
    public int deletePterodactylNodeFromNodeId(String nodeId) {
        try {
            return jdbcTemplate.update("""
                DELETE FROM pterodactyl_node_
                WHERE node_id = ?
                """,
                nodeId
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
    
    // --- DNS A Record operations ---
    
    public int insertDnsARecord(DnsARecord dnsARecord) {
        try {
            return jdbcTemplate.update("""
                INSERT INTO dns_a_record_ (
                    node_id,
                    a_record_id,
                    zone_id,
                    zone_name,
                    record_name,
                    content
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                dnsARecord.nodeId(),
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
    
    public Optional<DnsARecord> selectDnsARecord(String nodeId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT
                    node_id,
                    a_record_id,
                    zone_id,
                    zone_name,
                    record_name,
                    content
                FROM dns_a_record_
                WHERE node_id = ?
                """,
                (rs, rowNum) -> new DnsARecord(
                    rs.getString("node_id"),
                    rs.getString("a_record_id"),
                    rs.getString("zone_id"),
                    rs.getString("zone_name"),
                    rs.getString("record_name"),
                    rs.getString("content")),
                    nodeId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to fetch DNS A record for node id %s", nodeId), e);
        }
    }

    public List<DnsARecord> selectAllARecordIds() {
        try {
            return jdbcTemplate.query(
                """
                SELECT
                    node_id,
                    a_record_id,
                    zone_id,
                    zone_name,
                    record_name,
                    content
                FROM dns_a_record_
                """,
                (rs, rowNum) -> new DnsARecord(
                    rs.getString("node_id"),
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
    
    public int deleteDnsARecord(String nodeId) {
        try {
            return jdbcTemplate.update("""
                DELETE FROM dns_a_record_
                WHERE a_record_id = ?
                """,
                nodeId
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to delete DNS A record: " + e.getMessage(), e);
        }
    }
}