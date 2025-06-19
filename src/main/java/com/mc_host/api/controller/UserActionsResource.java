package com.mc_host.api.controller;

import com.mc_host.api.model.subscription.request.UpdateAddressRequest;
import com.mc_host.api.model.subscription.request.UpdateTitleRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/")
public interface UserActionsResource {
    
    @PostMapping("/user/{userId}/subscription/{subscriptionId}/title")
    public ResponseEntity<Void> updateSubscriptionTitle(
        @PathVariable String userId,
        @PathVariable String subscriptionId,
        @RequestBody UpdateTitleRequest title
    );

    @PostMapping("/user/{userId}/subscription/{subscriptionId}/address")
    public ResponseEntity<Void> updateSubscriptionAddress(
        @PathVariable String userId,
        @PathVariable String subscriptionId,
        @RequestBody UpdateAddressRequest address
    );
}
