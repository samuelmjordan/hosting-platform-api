package com.mc_host.api.controller.api;

import com.mc_host.api.auth.ValidatedSubscription;
import com.mc_host.api.model.server.response.ProvisioningStatusResponse;
import com.mc_host.api.model.stripe.request.UpdateSpecificationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/user/subscription/{subscriptionId}")
public interface SubscriptionController {

    @PostMapping("/cancel")
    ResponseEntity<Void> cancelSubscription(
        @ValidatedSubscription String subscriptionId
    );

    @PostMapping("uncancel")
    ResponseEntity<Void> uncancelSubscription(
        @ValidatedSubscription String subscriptionId
    );

    @PostMapping("specification")
    ResponseEntity<Void> updateSubscriptionSpecification(
        @ValidatedSubscription String subscriptionId,
        @RequestBody UpdateSpecificationRequest specificationRequest
    );

    @GetMapping("status")
    ResponseEntity<ProvisioningStatusResponse> getProvisioningStatus(
        @ValidatedSubscription String subscriptionId
    );
}
