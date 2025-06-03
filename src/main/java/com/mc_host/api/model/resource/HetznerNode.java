package com.mc_host.api.model.resource;

import com.mc_host.api.model.resource.hetzner.HetznerRegion;

public record HetznerNode(
    String subscriptionId,
    Long nodeId,
    HetznerRegion hetznerRegion,
    String ipv4
) {}

