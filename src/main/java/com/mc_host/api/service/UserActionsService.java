package com.mc_host.api.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mc_host.api.controller.UserActionsResource;
import com.mc_host.api.model.cache.StripeEventType;
import com.mc_host.api.model.resource.DnsCNameRecord;
import com.mc_host.api.model.subscription.ContentSubscription;
import com.mc_host.api.model.subscription.request.UpdateAddressRequest;
import com.mc_host.api.model.subscription.request.UpdateRegionRequest;
import com.mc_host.api.model.subscription.request.UpdateTitleRequest;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.repository.SubscriptionRepository;
import com.mc_host.api.service.resources.DnsService;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.Status;
import com.mc_host.api.service.stripe.events.StripeEventProcessor;

@Service
public class UserActionsService implements UserActionsResource {
    //private final static Logger LOGGER = Logger.getLogger(UserActionsService.class.getName());

    private final GameServerRepository gameServerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ServerExecutionContextRepository serverExecutionContextRepository;
    private final StripeEventProcessor stripeEventProcessor;
    private final DnsService dnsService;

    public UserActionsService(
        GameServerRepository gameServerRepository,
        SubscriptionRepository subscriptionRepository,
        ServerExecutionContextRepository serverExecutionContextRepository,
        StripeEventProcessor stripeEventProcessor,
        DnsService dnsService
    ) {
        this.gameServerRepository = gameServerRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.serverExecutionContextRepository = serverExecutionContextRepository;
        this.stripeEventProcessor = stripeEventProcessor;
        this.dnsService = dnsService;
    }

    @Override
    public ResponseEntity<Void> updateSubscriptionTitle(String userId, String subscriptionId, UpdateTitleRequest title) {
        serverExecutionContextRepository.updateTitle(subscriptionId, title.title());
        return ResponseEntity.ok().build();
    }

    @Override
    @Transactional
    public ResponseEntity<Void> updateSubscriptionAddress(String userId, String subscriptionId, UpdateAddressRequest address) {
        Context context = serverExecutionContextRepository.selectSubscription(subscriptionId)
            .orElseThrow(() -> new IllegalStateException("No context found for subscription " + subscriptionId));
        if (context.getStatus().equals(Status.IN_PROGRESS)) {
            return ResponseEntity.status(409)
                .header("X-Reason", "Server currently provisioning")
                .build();
        }

        DnsCNameRecord dnsCNameRecord = gameServerRepository.selectDnsCNameRecord(context.getCNameRecordId())
            .orElseThrow(() -> new IllegalStateException("DNS CNAME record not found: " + context.getCNameRecordId()));   
        String newURL = String.join(".", address.address(), dnsCNameRecord.zoneName());
        Boolean nameTaken = gameServerRepository.isDnsCNameRecordNameTaken(newURL, dnsCNameRecord.zoneId());
        if (nameTaken) {
            return ResponseEntity.status(409)
                .header("X-Reason", "URL taken")
                .build();
        }
   
        DnsCNameRecord newDnsCNameRecord = dnsService.updateCNameRecordName(dnsCNameRecord, address.address());
        gameServerRepository.updateDnsCNameRecord(newDnsCNameRecord);

        String customerId = subscriptionRepository.selectSubscription(subscriptionId)
            .map(ContentSubscription::customerId)
            .orElseThrow(() -> new IllegalStateException("No subscription found " + subscriptionId));
        stripeEventProcessor.enqueueEvent(StripeEventType.SUBSCRIPTION, customerId);
        return ResponseEntity.ok().build();
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
