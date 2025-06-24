package com.mc_host.api.model.server.response;

public record ResourceLimitResponse(
	String subscriptionId,
	Integer memory,
	Integer swap,
	Integer disk,
	Integer io,
	Integer cpu,
	Integer threads
) {
}
