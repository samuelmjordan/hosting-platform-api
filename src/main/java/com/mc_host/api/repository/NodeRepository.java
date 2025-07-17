package com.mc_host.api.repository;

import com.mc_host.api.model.resource.hetzner.HetznerCloudProduct;
import com.mc_host.api.model.resource.hetzner.HetznerRegion;
import com.mc_host.api.model.resource.hetzner.node.HetznerClaim;
import com.mc_host.api.model.resource.hetzner.node.HetznerCloudNode;
import com.mc_host.api.model.resource.hetzner.node.HetznerDedicatedNode;
import com.mc_host.api.model.resource.hetzner.node.HetznerNode;
import com.mc_host.api.model.resource.hetzner.node.HetznerNodeInterface;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Service
public class NodeRepository extends BaseRepository {

    public NodeRepository(JdbcTemplate jdbc) { super(jdbc); }

    public int insertCloudNode(HetznerCloudNode cloudNode) {
        insertNode(cloudNode);
        return execute("""
            INSERT INTO cloud_node_ (
                hetzner_node_id,
                cloud_product)
            VALUES (?, ?)
            """,
            cloudNode.hetznerNodeId(),
            cloudNode.cloudProduct().toString()
        );
    }

    private int insertNode(HetznerNodeInterface node) {
        return execute("""
            INSERT INTO node_ (
                hetzner_node_id,
                hetzner_region,
                ipv4,
                dedicated)
            VALUES (?, ?, ?, ?)
            """,
            node.hetznerNodeId(),
            node.hetznerRegion().toString(),
            node.ipv4(),
            node.dedicated()
        );
    }

    public int insertClaim(HetznerClaim claim) {
        return execute("""
            INSERT INTO resource_claim_ (
                subscription_id,
                hetzner_node_id,
                ram_gb)
            VALUES (?, ?, ?)
            """,
            claim.subscriptionId(),
            claim.hetznerNodeId(),
            claim.ramGb());
    }

    public Optional<HetznerNode> selectHetznerNode(Long nodeId) {
        return selectOne("""
            SELECT
               hetzner_node_id,
               hetzner_region,
               ipv4,
               dedicated
            FROM node_
            WHERE hetzner_node_id = ?;
            """,
            this::mapHetznerNode,
            nodeId
        );
    }

    public Optional<HetznerCloudNode> selectHetznerCloudNode(Long nodeId) {
        return selectOne("""
            SELECT
               n.hetzner_node_id,
               n.hetzner_region,
               n.ipv4,
               c.cloud_product
            FROM node_ n
            JOIN cloud_node_ c ON n.hetzner_node_id = c.hetzner_node_id
            WHERE n.hetzner_node_id = ?;
            """,
            this::mapHetznerCloudNode,
            nodeId
        );
    }

    public Optional<HetznerClaim> selectClaim(String subscriptionId) {
        return selectOne("""
            SELECT
                subscription_id,
                hetzner_node_id,
                ram_gb
            FROM resource_claim_
            WHERE subscription_id = ?;
            """,
            this::mapHetznerClaim,
            subscriptionId
        );
    }

    public List<Long> selectAllCloudHetznerNodeIds() {
        return selectMany("SELECT hetzner_node_id FROM cloud_node_",
                (rs, rowNum) -> rs.getLong("hetzner_node_id"));
    }

    public int deleteClaim(String subscriptionId, Long hetznerNodeId) {
        return execute("DELETE FROM resource_claim_ WHERE subscription_id = ? AND hetzner_node_id = ?",
            subscriptionId, hetznerNodeId);
    }

    public int deleteHetznerNode(Long hetznerNodeId) {
        return execute("DELETE FROM node_ WHERE hetzner_node_id = ?", hetznerNodeId);
    }

    public int deleteHetznerCloudNode(Long hetznerNodeId) {
        return execute("DELETE FROM node_ WHERE hetzner_node_id = ? AND dedicated = false", hetznerNodeId);
    }

    // mappers
    private HetznerNode mapHetznerNode(ResultSet rs, int rowNum) throws SQLException {
        return new HetznerNode(
            rs.getLong("hetzner_node_id"),
            HetznerRegion.lookup(rs.getString("hetzner_region")),
            rs.getString("ipv4"),
            rs.getBoolean("dedicated")
        );
    }

    private HetznerCloudNode mapHetznerCloudNode(ResultSet rs, int rowNum) throws SQLException {
        return new HetznerCloudNode(
            rs.getLong("hetzner_node_id"),
            HetznerRegion.lookup(rs.getString("hetzner_region")),
            rs.getString("ipv4"),
            HetznerCloudProduct.lookup(rs.getString("cloud_product"))
        );
    }

    private HetznerDedicatedNode mapHetznerDedicatedNode(ResultSet rs, int rowNum) throws SQLException {
        return new HetznerDedicatedNode(
            rs.getLong("hetzner_node_id"),
            HetznerRegion.lookup(rs.getString("hetzner_region")),
            rs.getString("ipv4"),
            rs.getString("dedicated_product"),
            rs.getLong("total_ram_gb"),
            rs.getBoolean("active")
        );
    }

    private HetznerClaim mapHetznerClaim(ResultSet rs, int rowNum) throws SQLException {
        return new HetznerClaim(
            rs.getString("subscription_id"),
            rs.getLong("hetzner_node_id"),
            rs.getLong("ram_gb")
        );
    }
}