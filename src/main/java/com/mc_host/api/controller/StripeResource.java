package com.mc_host.api.controller;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.javapoet.ClassName;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mc_host.api.configuration.StripeConfiguration;
import com.mc_host.api.service.StripeEventProcessor;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;

@RestController
@RequestMapping("/api/webhook")
public class StripeResource {
    private static final Logger LOGGER = Logger.getLogger(ClassName.class.getName());
    
    private final StripeConfiguration stripeConfig;
    private final StripeEventProcessor stripeEventProcessor;

    public StripeResource(
        StripeConfiguration configuration,
        StripeEventProcessor eventProcessor
    ) {
        this.stripeConfig = configuration;
        this.stripeEventProcessor = eventProcessor;
    }

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload, 
                                                    @RequestHeader("Stripe-Signature") String sigHeader) {
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
