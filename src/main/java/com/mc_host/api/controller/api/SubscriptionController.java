package com.mc_host.api.controller.api;

import com.mc_host.api.auth.ValidatedSubscription;
import com.mc_host.api.model.stripe.request.UpdateSpecificationRequest;
import com.mc_host.api.model.subscription.ContentSubscription;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/user/subscription/{subscriptionId}")
public interface SubscriptionController {

    @PostMapping("/cancel")
    public ResponseEntity<Void> cancelSubscription(
        @ValidatedSubscription String subscriptionId
    );

    @PostMapping("uncancel")
    public ResponseEntity<Void> uncancelSubscription(
        @ValidatedSubscription String subscriptionId
    );

    @PostMapping("specification")
    public ResponseEntity<Void> updateSubscriptionSpecification(
        @ValidatedSubscription ContentSubscription subscription,
        @RequestBody UpdateSpecificationRequest specificationRequest
    );
}
