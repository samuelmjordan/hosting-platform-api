package com.mc_host.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mc_host.api.model.request.CheckoutRequest;
import com.mc_host.api.model.request.PortalRequest;

@RestController
@RequestMapping("/api/stripe")
public interface StripeResource {

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
        @RequestBody String payload, 
        @RequestHeader("Stripe-Signature") String sigHeader
    );

    @PostMapping("/billing-portal")
    public ResponseEntity<String> userPortal(
        @RequestBody PortalRequest request
    );

    @PostMapping("/checkout")
    public ResponseEntity<String> startCheckout(
        @RequestBody CheckoutRequest request
    );

    @PostMapping("/user/{userId}/subscription/{subscriptionId}/cancel")
    public ResponseEntity<Void> cancelSubscription(
        @PathVariable String userId,
        @PathVariable String subscriptionId
    );

    @PostMapping("/user/{userId}/subscription/{subscriptionId}/uncancel")
    public ResponseEntity<Void> uncancelSubscription(
        @PathVariable String userId,
        @PathVariable String subscriptionId
    );
}
