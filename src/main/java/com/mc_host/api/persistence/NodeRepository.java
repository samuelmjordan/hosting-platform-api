package com.mc_host.api.persistence;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mc_host.api.model.entity.node.Node;

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
                        node_id
                    )
                    VALUES (?)
                    """);
                ps.setString(1, node.getNodeId());
                return ps;
            });
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to create new node: " + e.getMessage(), e);
        }
    }

    public int updateNode(Node node) {
        try {
            return jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement("""
                    UPDATE node_
                    SET pterodactyl_node_id = ?,
                        hetzner_node_id = ?,
                        ipv4 = ?,
                        hetzner_region = ?
                    WHERE node_id = ?
                    """);
                ps.setString(1, node.getPterodactylNodeId());
                ps.setLong(2, node.getHetznerNodeId());
                ps.setString(3, node.getIpv4());
                ps.setString(4, node.getHetznerRegion().toString());
                ps.setString(5, node.getNodeId());
                return ps;
            });
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to create new node: " + e.getMessage(), e);
        }
    }
    
}
