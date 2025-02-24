package com.mc_host.api.model.node;

import java.util.UUID;

import com.mc_host.api.model.hetzner.HetznerRegion;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Node {
    private final String nodeId;
    private final Boolean dedicated;

    private Long pterodactylNodeId;
    private Long hetznerNodeId;
    private String ipv4;
    private HetznerRegion hetznerRegion;

    private Node(Boolean dedicated) {
        this.nodeId  =  UUID.randomUUID().toString();
        this.dedicated = dedicated;
    }

    public static Node newCloudNode() {
        return new Node(false);
    }

    public static Node newDedicatedNode() {
        return new Node(true);
    }

    public String getSubdomain() {
        return nodeId.replace("-", "");
    }
}
