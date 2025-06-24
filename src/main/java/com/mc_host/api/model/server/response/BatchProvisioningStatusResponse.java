package com.mc_host.api.model.server.response;

import java.util.List;

public record BatchProvisioningStatusResponse(
	List<ProvisioningStatusResponse> statuses,
	List<BatchError> errors
) {
}
