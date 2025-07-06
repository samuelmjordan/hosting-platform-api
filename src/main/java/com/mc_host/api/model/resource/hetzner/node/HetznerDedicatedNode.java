package com.mc_host.api.model.resource.hetzner.node;

import com.mc_host.api.model.resource.hetzner.HetznerRegion;

public record HetznerDedicatedNode(
	Long hetznerNodeId,
	HetznerRegion hetznerRegion,
	String ipv4,
	String dedicatedProduct,
	Long totalRamGb,
	Boolean active
) implements HetznerNodeInterface {
	public Boolean dedicated() {return true;}
}
