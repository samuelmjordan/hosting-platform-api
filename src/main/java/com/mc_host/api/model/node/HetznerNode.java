package com.mc_host.api.model.node;

import com.mc_host.api.model.hetzner.HetznerRegion;

public record HetznerNode(
    String nodeId,
    Long hetznerNodeId,
    HetznerRegion hetznerRegion,
    String ipv4
) {}

