package com.mc_host.api.controller.webhook;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("stripe")
public interface StripeWebhookController {

	@PostMapping("webhook")
	public ResponseEntity<String> handleStripeWebhook(
		@RequestBody String payload,
		@RequestHeader("Stripe-Signature") String sigHeader
	);
}
