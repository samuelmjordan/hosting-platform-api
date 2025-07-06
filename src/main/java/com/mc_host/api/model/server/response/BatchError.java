package com.mc_host.api.model.server.response;

public record BatchError(
	String subscriptionId,
	Integer responseCode,
	String errorMessage
) {
}
