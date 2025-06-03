package com.mc_host.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mc_host.api.model.request.UpdateAddressRequest;
import com.mc_host.api.model.request.UpdateRegionRequest;
import com.mc_host.api.model.request.UpdateTitleRequest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


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

    @PostMapping("/user/{userId}/subscription/{subscriptionId}/region")
    public ResponseEntity<Void> updateSubscriptionRegion(
        @PathVariable String userId,
        @PathVariable String subscriptionId,
        @RequestBody UpdateRegionRequest region
    );
}
