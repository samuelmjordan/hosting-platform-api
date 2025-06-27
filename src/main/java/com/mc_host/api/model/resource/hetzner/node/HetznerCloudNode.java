package com.mc_host.api.model.resource.hetzner.node;

import com.mc_host.api.model.resource.hetzner.HetznerCloudProduct;
import com.mc_host.api.model.resource.hetzner.HetznerRegion;

public record HetznerCloudNode(
	Long hetznerNodeId,
	HetznerRegion hetznerRegion,
	String ipv4,
	HetznerCloudProduct cloudProduct
) implements HetznerNodeInterface {
	public Boolean dedicated() {return false;}
}
