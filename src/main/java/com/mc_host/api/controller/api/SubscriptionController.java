package com.mc_host.api.controller.api;

import com.mc_host.api.auth.CurrentUser;
import com.mc_host.api.model.stripe.request.UpdateSpecificationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/user/subscription/{subscriptionId}")
public interface SubscriptionController {

    @PostMapping("/cancel")
    public ResponseEntity<Void> cancelSubscription(
        @CurrentUser String clerkId,
        @PathVariable String subscriptionId
    );

    @PostMapping("uncancel")
    public ResponseEntity<Void> uncancelSubscription(
        @CurrentUser String clerkId,
        @PathVariable String subscriptionId
    );

    @PostMapping("specification")
    public ResponseEntity<Void> updateSubscriptionSpecification(
        @CurrentUser String clerkId,
        @PathVariable String subscriptionId,
        @RequestBody UpdateSpecificationRequest specificationRequest
    );
}
