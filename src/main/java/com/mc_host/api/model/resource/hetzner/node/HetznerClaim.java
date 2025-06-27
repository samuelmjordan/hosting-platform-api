package com.mc_host.api.model.resource.hetzner.node;

public record HetznerClaim(
	String subscriptionId,
	Long hetznerNodeId,
	Long ramGb
) {
}
