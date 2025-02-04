package com.mc_host.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stripe")
public interface StripeResource {

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
        @RequestBody String payload, 
        @RequestHeader("Stripe-Signature") String sigHeader);

    @GetMapping("/customerId/{userId}")
    public ResponseEntity<String> getCustomerId(
        @PathVariable String userId);
}
