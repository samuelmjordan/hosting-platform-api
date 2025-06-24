package com.mc_host.api.controller.api;

import com.mc_host.api.model.server.response.BatchResourceLimitResponse;
import com.mc_host.api.model.server.response.ResourceLimitResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/dashboard")
public interface ResourceLimitResource {

	@GetMapping("/user/{userId}/subscription/{subscriptionId}/limits")
	ResponseEntity<ResourceLimitResponse> getProvisioningStatus(
		@PathVariable String userId,
		@PathVariable String subscriptionId
	);

	@PostMapping("/user/{userId}/subscriptions/limits")
	ResponseEntity<BatchResourceLimitResponse> getBatchProvisioningStatus(
		@PathVariable String userId,
		@RequestBody List<String> subscriptionIds
	);
}
