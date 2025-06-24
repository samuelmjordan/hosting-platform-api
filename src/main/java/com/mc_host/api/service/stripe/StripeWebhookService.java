package com.mc_host.api.service.stripe;

import com.mc_host.api.configuration.StripeConfiguration;
import com.mc_host.api.controller.webhook.StripeWebhookController;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class StripeWebhookService implements StripeWebhookController {
	private static final Logger LOGGER = Logger.getLogger(StripeWebhookService.class.getName());

	private final StripeConfiguration stripeConfiguration;
	private final StripeEventProcessor stripeEventProcessor;
	private final Executor virtualThreadExecutor;

	@Override
	public ResponseEntity<String> handleStripeWebhook(String payload, String sigHeader) {
		try {
			Event event = Webhook.constructEvent(payload, sigHeader, stripeConfiguration.getSigningKey());
			LOGGER.log(Level.FINE, String.format(
				"[Thread: %s] Stripe Event Received - Type: %s, ID: %s",
				Thread.currentThread().getName(),
				event.getType(),
				event.getId()
			));
			virtualThreadExecutor.execute(() -> stripeEventProcessor.processEvent(event));
			return ResponseEntity.ok().body("Webhook Received");
		} catch (SignatureVerificationException e) {
			LOGGER.log(Level.SEVERE, "Invalid signature", e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Unexpected error processing webhook", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
		}
	}
}
