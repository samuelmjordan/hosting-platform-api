package com.mc_host.api.controller.api;

import com.mc_host.api.auth.ValidatedSubscription;
import com.mc_host.api.model.subscription.request.UpdateAddressRequest;
import com.mc_host.api.model.subscription.request.UpdateTitleRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/")
public interface UserActionsResource {
    
    @PostMapping("/user/subscription/{subscriptionId}/title")
    public ResponseEntity<Void> updateSubscriptionTitle(
        @ValidatedSubscription String subscriptionId,
        @RequestBody UpdateTitleRequest title
    );

    @PostMapping("/user/subscription/{subscriptionId}/address")
    public ResponseEntity<Void> updateSubscriptionAddress(
        @ValidatedSubscription String subscriptionId,
        @RequestBody UpdateAddressRequest address
    );
}
