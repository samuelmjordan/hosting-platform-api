package com.mc_host.api.persistence;

import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mc_host.api.model.hetzner.HetznerRegion;
import com.mc_host.api.model.node.Node;

@Service
public class NodeRepository {

    private final JdbcTemplate jdbcTemplate;

    public NodeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int insertNewNode(Node node) {
        try {
            return jdbcTemplate.update("""
                INSERT INTO node_ (
                    node_id,
                    dedicated
                )
                VALUES (?, ?)
                """,
                node.getNodeId(),
                node.getDedicated()
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to create new node: " + e.getMessage(), e);
        }
    }

    public int updateNode(Node node) {
        try {
            return jdbcTemplate.update("""
                UPDATE node_
                SET dedicated = ?,
                    pterodactyl_node_id = ?,
                    hetzner_node_id = ?,
                    hetzner_region = ?,
                    a_record_id = ?,
                    zone_name = ?,
                    record_name = ?,
                    ipv4 = ?
                WHERE node_id = ?
                """,
                node.getDedicated(),
                node.getPterodactylNodeId(),
                node.getHetznerNodeId(),
                node.getHetznerRegion().toString(),
                node.getARecordId(),
                node.getZoneName(),
                node.getRecordName(),
                node.getIpv4(),
                node.getNodeId()
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to update node: " + e.getMessage(), e);
        }
    }

        public Optional<Node> selectNode(String nodeId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT
                    node_id,
                    dedicated,
                    pterodactyl_node_id,
                    hetzner_node_id,
                    hetzner_region,
                    a_record_id,
                    zone_name,
                    record_name,
                    ipv4
                FROM node_
                WHERE node_id = ?
                """,
                (rs, rowNum) -> new Node(
                    rs.getString("node_id"), 
                    rs.getBoolean("dedicated"), 
                    rs.getLong("pterodactyl_node_id"),
                    rs.getLong("hetzner_node_id"), 
                    HetznerRegion.valueOf(rs.getString("hetzner_region")),
                    rs.getString("a_record_id"),
                    rs.getString("zone_name"),
                    rs.getString("record_name"),
                    rs.getString("ipv4")),
                    nodeId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException(String.format("Failed to fetch node for node id %s", nodeId), e);
        }
    }
    
}
