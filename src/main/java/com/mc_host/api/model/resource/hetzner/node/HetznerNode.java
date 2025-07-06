package com.mc_host.api.model.resource.hetzner.node;

import com.mc_host.api.model.resource.hetzner.HetznerRegion;

public record HetznerNode (
    Long hetznerNodeId,
    HetznerRegion hetznerRegion,
    String ipv4,
	Boolean dedicated
) implements HetznerNodeInterface {}

