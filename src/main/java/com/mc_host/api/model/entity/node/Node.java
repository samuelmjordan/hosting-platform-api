package com.mc_host.api.model.entity.node;

import java.util.UUID;

import com.mc_host.api.model.hetzner.HetznerRegion;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Node {
    private final String nodeId;

    private String pterodactylNodeId;
    private Long hetznerNodeId;
    private String ipv4;
    private HetznerRegion hetznerRegion;

    public Node() {
        this.nodeId  =  UUID.randomUUID().toString();
    }
}
