package com.mc_host.api.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.mc_host.api.controller.UserActionsResource;
import com.mc_host.api.model.request.UpdateAddressRequest;
import com.mc_host.api.model.request.UpdateRegionRequest;
import com.mc_host.api.model.request.UpdateTitleRequest;
import com.mc_host.api.repository.ServerExecutionContextRepository;

@Service
public class UserActionsService implements UserActionsResource {

    private final ServerExecutionContextRepository serverExecutionContextRepository;

    public UserActionsService(
        ServerExecutionContextRepository serverExecutionContextRepository
    ) {
        this.serverExecutionContextRepository = serverExecutionContextRepository;
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
    public ResponseEntity<Void> updateSubscriptionAdress(String userId, String subscriptionId, UpdateAddressRequest address) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateSubscriptionAdress'");
    }

    @Override
    public ResponseEntity<Void> updateSubscriptionRegion(String userId, String subscriptionId, UpdateRegionRequest region) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateSubscriptionRegion'");
    }
    
}
