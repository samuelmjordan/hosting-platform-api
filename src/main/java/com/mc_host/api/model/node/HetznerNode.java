package com.mc_host.api.model.node;

import com.mc_host.api.model.hetzner.HetznerRegion;

public record HetznerNode(
    String subscriptionId,
    Long nodeId,
    HetznerRegion hetznerRegion,
    String ipv4
) {}

