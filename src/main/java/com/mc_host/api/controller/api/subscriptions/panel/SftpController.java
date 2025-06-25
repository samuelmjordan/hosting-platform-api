package com.mc_host.api.controller.api.subscriptions.panel;

import com.mc_host.api.auth.CurrentUser;
import com.mc_host.api.auth.ValidatedSubscription;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/panel/user/subscription/{subscriptionId}/sftp")
public interface SftpController {

	@GetMapping("credentials")
	ResponseEntity<CredentialsResponse> getCredentials(
		@CurrentUser String userId,
		@ValidatedSubscription String subscriptionId
	);

	record CredentialsResponse(
		String connectionString,
		String username,
		String encryptedPassword,
		Integer port
	) {}
}
