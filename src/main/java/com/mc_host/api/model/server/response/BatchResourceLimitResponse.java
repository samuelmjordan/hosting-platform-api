package com.mc_host.api.model.server.response;

import java.util.List;

public record BatchResourceLimitResponse(
	List<ResourceLimitResponse> limits,
	List<BatchError> errors
) {
}
