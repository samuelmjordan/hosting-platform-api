package com.mc_host.api.service;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.mc_host.api.configuration.StripeConfiguration;
import com.mc_host.api.controller.StripeResource;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;

@Service
public class StripeWebhookService implements StripeResource{
    private static final Logger LOGGER = Logger.getLogger(StripeWebhookService.class.getName());
    
    private final StripeConfiguration stripeConfig;
    private final StripeEventProcessor stripeEventProcessor;

    public StripeWebhookService(
        StripeConfiguration configuration,
        StripeEventProcessor eventProcessor
    ) {
        this.stripeConfig = configuration;
        this.stripeEventProcessor = eventProcessor;
    }

    @Override
    public ResponseEntity<String> handleStripeWebhook(String payload, String sigHeader) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, stripeConfig.getSigningKey());
            LOGGER.log(Level.INFO, String.format(
                "[Thread: %s] Stripe Event Received - Type: %s, ID: %s",
                Thread.currentThread().getName(),
                event.getType(),
                event.getId()
            ));
            stripeEventProcessor.processEvent(event);
            return ResponseEntity.ok().body("Webhook Received");
        } catch (SignatureVerificationException e) {
            LOGGER.log(Level.SEVERE, "Invalid signature", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,"Error processing webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook processing failed");
        }
    }
}
