package com.mc_host.api.model.resource.hetzner.node;

import com.mc_host.api.model.resource.hetzner.HetznerRegion;

public interface HetznerNodeInterface {
	Long hetznerNodeId();
	HetznerRegion hetznerRegion();
	String ipv4();
	Boolean dedicated();
}
