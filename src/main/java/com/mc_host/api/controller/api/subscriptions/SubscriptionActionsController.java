package com.mc_host.api.controller.api.subscriptions;

import com.mc_host.api.auth.ValidatedSubscription;
import com.mc_host.api.model.stripe.request.UpdateSpecificationRequest;
import com.mc_host.api.model.subscription.request.UpdateAddressRequest;
import com.mc_host.api.model.subscription.request.UpdateTitleRequest;
import com.mc_host.api.model.subscription.request.UpgradeConfirmationResponse;
import com.mc_host.api.model.subscription.request.UpgradePreviewResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("api/user/subscription/{subscriptionId}")
public interface SubscriptionActionsController {

    @PostMapping("cancel")
    ResponseEntity<Void> cancelSubscription(
        @ValidatedSubscription String subscriptionId
    );

    @PostMapping("uncancel")
    ResponseEntity<Void> uncancelSubscription(
        @ValidatedSubscription String subscriptionId
    );

    @PostMapping("specification/preview")
    ResponseEntity<UpgradePreviewResponse> previewSubscriptionSpecification(
        @ValidatedSubscription String subscriptionId,
        @RequestBody UpdateSpecificationRequest specificationRequest
    );

    @PostMapping("specification")
    ResponseEntity<UpgradeConfirmationResponse> updateSubscriptionSpecification(
        @ValidatedSubscription String subscriptionId,
        @RequestBody UpdateSpecificationRequest specificationRequest
    );
    
    @PostMapping("title")
    public ResponseEntity<Void> updateSubscriptionTitle(
        @ValidatedSubscription String subscriptionId,
        @RequestBody UpdateTitleRequest title
    );

    @PostMapping("address")
    public ResponseEntity<Void> updateSubscriptionAddress(
        @ValidatedSubscription String subscriptionId,
        @RequestBody UpdateAddressRequest address
    );
}
