package com.mc_host.api.model.server.response;

import com.mc_host.api.model.server.ProvisioningStatus;

import java.util.Map;

public record BatchProvisioningStatusResponse(
	Map<String, ProvisioningStatus> statuses,
	Map<String, String> errors
) {
}
