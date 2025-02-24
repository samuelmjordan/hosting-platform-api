package com.mc_host.api.persistence;

import java.sql.Types;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mc_host.api.model.node.Node;

@Service
public class NodeRepository {

    private final JdbcTemplate jdbcTemplate;

    public NodeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int insertNewNode(Node node) {
        try {
            return jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement("""
                    INSERT INTO node_ (
                        node_id,
                        dedicated
                    )
                    VALUES (?, ?)
                    """);
                ps.setString(1, node.getNodeId());
                ps.setBoolean(2, node.getDedicated());
                return ps;
            });
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to create new node: " + e.getMessage(), e);
        }
    }

    public int updateNode(Node node) {
        try {
            return jdbcTemplate.update("""
                UPDATE node_
                SET pterodactyl_node_id = ?,
                    hetzner_node_id = ?,
                    ipv4 = ?,
                    hetzner_region = ?
                WHERE node_id = ?
                """,
                node.getPterodactylNodeId(),
                node.getHetznerNodeId(),
                node.getIpv4(),
                node.getHetznerRegion().toString(),
                node.getNodeId()
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to update node: " + e.getMessage(), e);
        }
    }
    
}
