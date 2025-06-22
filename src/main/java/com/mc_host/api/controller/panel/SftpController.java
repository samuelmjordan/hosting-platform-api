package com.mc_host.api.controller.panel;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/panel/user/{userId}/subscription/{subscriptionId}/sftp")
public interface SftpController {

	@GetMapping("/credentials")
	ResponseEntity<CredentialsResponse> getCredentials(
		@PathVariable String userId,
		@PathVariable String subscriptionId
	);

	record CredentialsResponse(
		String connectionString,
		String username,
		String encryptedPassword,
		Integer port
	) {}
}
