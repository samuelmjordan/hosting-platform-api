package com.mc_host.api.controller.api;

import com.mc_host.api.ValidatedPaymentMethod;
import com.mc_host.api.auth.CurrentUser;
import com.mc_host.api.model.stripe.request.CreatePaymentMethodRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stripe/user/payment-method")
public interface PaymentMethodResource {

    @PostMapping("/{paymentMethodId}/default")
    public ResponseEntity<Void> setDefaultPaymentMethod(
        @ValidatedPaymentMethod String paymentMethodId
    );

    @PostMapping("/{paymentMethodId}/default/remove")
    public ResponseEntity<Void> removeDefaultPaymentMethod(
        @ValidatedPaymentMethod String paymentMethodId
    );

    @PostMapping("/{paymentMethodId}/remove")
    public ResponseEntity<Void> removePaymentMethod(
        @ValidatedPaymentMethod String paymentMethodId
    );

    @PostMapping
    public ResponseEntity<String> createPaymentMethod(
        @CurrentUser String userId,
        @RequestBody CreatePaymentMethodRequest request
    );
}
