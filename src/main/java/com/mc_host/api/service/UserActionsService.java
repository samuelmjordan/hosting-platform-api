package com.mc_host.api.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mc_host.api.controller.UserActionsResource;
import com.mc_host.api.model.cache.StripeEventType;
import com.mc_host.api.model.entity.ContentSubscription;
import com.mc_host.api.model.request.UpdateAddressRequest;
import com.mc_host.api.model.request.UpdateRegionRequest;
import com.mc_host.api.model.request.UpdateTitleRequest;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.repository.SubscriptionRepository;
import com.mc_host.api.service.stripe.StripeEventProcessor;
import com.mc_host.api.service.stripe.StripeService;

@Service
public class UserActionsService implements UserActionsResource {

    private final SubscriptionRepository subscriptionRepository;
    private final ServerExecutionContextRepository serverExecutionContextRepository;
    private final StripeEventProcessor stripeEventProcessor;

    public UserActionsService(
        SubscriptionRepository subscriptionRepository,
        ServerExecutionContextRepository serverExecutionContextRepository,
        StripeEventProcessor stripeEventProcessor
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.serverExecutionContextRepository = serverExecutionContextRepository;
        this.stripeEventProcessor = stripeEventProcessor;
    }

    @Override
    public ResponseEntity<Void> setDefaultPaymentMethod(String userId, String paymentMethodId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setDefaultPaymentMethod'");
    }

    @Override
    public ResponseEntity<Void> cancelSubscription(String userId, String subscriptionId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'cancelSubscription'");
    }

    @Override
    public ResponseEntity<Void> updateSubscriptionTitle(String userId, String subscriptionId, UpdateTitleRequest title) {
        serverExecutionContextRepository.updateTitle(subscriptionId, title.title());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> updateSubscriptionAddress(String userId, String subscriptionId, UpdateAddressRequest address) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateSubscriptionAdress'");
    }

    @Override
    @Transactional
    public ResponseEntity<Void> updateSubscriptionRegion(String userId, String subscriptionId, UpdateRegionRequest region) {
        String customerId = subscriptionRepository.selectSubscription(subscriptionId)
            .map(ContentSubscription::customerId)
            .orElseThrow(() -> new IllegalStateException("No subscription found " + subscriptionId));
        serverExecutionContextRepository.updateRegion(subscriptionId, region.region());
        stripeEventProcessor.enqueueEvent(StripeEventType.SUBSCRIPTION, customerId);
        return ResponseEntity.ok().build();
    }
    
}
