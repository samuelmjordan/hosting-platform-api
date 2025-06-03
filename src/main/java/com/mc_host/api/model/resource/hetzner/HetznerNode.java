package com.mc_host.api.model.resource.hetzner;

public record HetznerNode(
    String subscriptionId,
    Long nodeId,
    HetznerRegion hetznerRegion,
    String ipv4
) {}

