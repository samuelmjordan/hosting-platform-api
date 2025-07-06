package com.mc_host.api.controller.webhook;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("clerk")
public interface ClerkWebhookController {

	@PostMapping("webhook")
	public ResponseEntity<String> handleClerkWebhook(
		@RequestBody String payload,
		@RequestHeader("svix-id") String svixId,
		@RequestHeader("svix-timestamp") String svixTimestamp,
		@RequestHeader("svix-signature") String svixSignature
	);
}
