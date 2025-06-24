package com.mc_host.api.controller;

import com.mc_host.api.model.server.response.BatchProvisioningStatusResponse;
import com.mc_host.api.model.server.response.ProvisioningStatusResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public interface ProvisioningStatusResource {

	@GetMapping("/user/{userId}/subscription/{subscriptionId}/status")
	public ResponseEntity<ProvisioningStatusResponse> getProvisioningStatus(
		@PathVariable String userId,
		@PathVariable String subscriptionId
	);

	@PostMapping("/users/{userId}/subscriptions/status")
	ResponseEntity<BatchProvisioningStatusResponse> getBatchProvisioningStatus(
		@PathVariable String userId,
		@RequestBody List<String> subscriptionIds
	);
}
