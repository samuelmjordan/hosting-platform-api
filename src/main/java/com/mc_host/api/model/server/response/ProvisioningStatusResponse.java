package com.mc_host.api.model.server.response;

import com.mc_host.api.model.server.ProvisioningStatus;

public record ProvisioningStatusResponse(
	String subscriptionId,
    ProvisioningStatus status
) {
}
