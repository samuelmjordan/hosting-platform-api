package com.mc_host.api.controller.api.subscriptions.panel;

import com.mc_host.api.model.panel.startup.StartupResponse;
import com.mc_host.api.model.panel.startup.UpdateStartupRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/panel/user/{userId}/subscription/{subscriptionId}/settings")
public interface ServerSettingsResource {

	@GetMapping
	ResponseEntity<StartupResponse> getSettings(
		@PathVariable String userId,
		@PathVariable String subscriptionId
	);

	@PatchMapping
	ResponseEntity<StartupResponse> setSettings(
		@PathVariable String userId,
		@PathVariable String subscriptionId,
		@RequestBody UpdateStartupRequest request
	);

	@PostMapping
	ResponseEntity<Void> reinstallServer(
		@PathVariable String userId,
		@PathVariable String subscriptionId
	);

	@PostMapping("nuclear")
	ResponseEntity<Void> recreateServer(
		@PathVariable String userId,
		@PathVariable String subscriptionId
	);
}
