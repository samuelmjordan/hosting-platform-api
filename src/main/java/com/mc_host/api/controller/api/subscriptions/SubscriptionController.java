package com.mc_host.api.controller.api.subscriptions;

import com.mc_host.api.auth.ValidatedSubscription;
import com.mc_host.api.model.server.response.ProvisioningStatusResponse;
import com.mc_host.api.model.server.response.ResourceLimitResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/user/subscription/{subscriptionId}")
public interface SubscriptionController {

    @GetMapping("status")
    ResponseEntity<ProvisioningStatusResponse> getProvisioningStatus(
        @ValidatedSubscription String subscriptionId
    );

    @GetMapping("limits")
    ResponseEntity<ResourceLimitResponse> getResourceLimits(
        @ValidatedSubscription String subscriptionId
    );
}
