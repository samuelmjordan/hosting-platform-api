package com.mc_host.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mc_host.api.model.request.CreatePaymentMethodRequest;

@RestController
@RequestMapping("/api/stripe/user/{userId}/payment-method")
public interface PaymentMethodResource {

    @PostMapping("/{paymentMethodId}/default")
    public ResponseEntity<Void> setDefaultPaymentMethod(
        @PathVariable String userId,
        @PathVariable String paymentMethodId
    );

    @PostMapping("/{paymentMethodId}/default/remove")
    public ResponseEntity<Void> removeDefaultPaymentMethod(
        @PathVariable String userId,
        @PathVariable String paymentMethodId
    );

    @PostMapping("/{paymentMethodId}/remove")
    public ResponseEntity<Void> removePaymentMethod(
        @PathVariable String userId,
        @PathVariable String paymentMethodId
    );

    @PostMapping()
    public ResponseEntity<String> createPaymentMethod(
        @PathVariable String userId,
        @RequestBody CreatePaymentMethodRequest request
    );
    
}
