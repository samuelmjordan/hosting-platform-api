package com.mc_host.api.controller.api.subscriptions.panel;

import com.mc_host.api.auth.ValidatedSubscription;
import com.mc_host.api.model.panel.startup.StartupResponse;
import com.mc_host.api.model.panel.startup.UpdateStartupRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/panel/user/subscription/{subscriptionId}/settings")
public interface ServerSettingsController {

	@GetMapping
	ResponseEntity<StartupResponse> getSettings(
		@ValidatedSubscription String subscriptionId
	);

	@PatchMapping
	ResponseEntity<StartupResponse> setSettings(
		@ValidatedSubscription String subscriptionId,
		@RequestBody UpdateStartupRequest request
	);

	@PostMapping("reinstall")
	ResponseEntity<Void> reinstallServer(
		@ValidatedSubscription String subscriptionId
	);

	@PostMapping("nuclear")
	ResponseEntity<Void> recreateServer(
		@ValidatedSubscription String subscriptionId
	);
}
