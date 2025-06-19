package com.mc_host.api.service;

import com.mc_host.api.controller.UserActionsResource;
import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.Status;
import com.mc_host.api.model.resource.dns.DnsCNameRecord;
import com.mc_host.api.model.subscription.ContentSubscription;
import com.mc_host.api.model.subscription.request.UpdateAddressRequest;
import com.mc_host.api.model.subscription.request.UpdateRegionRequest;
import com.mc_host.api.model.subscription.request.UpdateTitleRequest;
import com.mc_host.api.queue.service.JobScheduler;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.repository.SubscriptionRepository;
import com.mc_host.api.service.resources.DnsService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.logging.Logger;

@Service
@Transactional
public class UserActionsService implements UserActionsResource {
    private final static Logger LOGGER = Logger.getLogger(UserActionsService.class.getName());

    private final GameServerRepository gameServerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ServerExecutionContextRepository serverExecutionContextRepository;
    private final JobScheduler jobScheduler;
    private final DnsService dnsService;

    public UserActionsService(
        GameServerRepository gameServerRepository,
        SubscriptionRepository subscriptionRepository,
        ServerExecutionContextRepository serverExecutionContextRepository,
        JobScheduler jobScheduler,
        DnsService dnsService
    ) {
        this.gameServerRepository = gameServerRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.serverExecutionContextRepository = serverExecutionContextRepository;
        this.jobScheduler = jobScheduler;
        this.dnsService = dnsService;
    }

    @Override
    public ResponseEntity<Void> updateSubscriptionTitle(String userId, String subscriptionId, UpdateTitleRequest title) {
        serverExecutionContextRepository.updateTitle(subscriptionId, title.title());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> updateSubscriptionAddress(String userId, String subscriptionId, UpdateAddressRequest address) {
        // TODO: this is shite
        // Should probably be queable. Should also add the adress to the context, currently its tied to the cname which can dissapear!
        // Or maybe the cname should be tied directly to the subscription? i guess it doesnt need to be taken down.
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
        jobScheduler.scheduleSubscriptionSync(customerId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> updateSubscriptionRegion(String userId, String subscriptionId, UpdateRegionRequest region) {
        String customerId = subscriptionRepository.selectSubscription(subscriptionId)
            .map(ContentSubscription::customerId)
            .orElseThrow(() -> new IllegalStateException("No subscription found " + subscriptionId));
        serverExecutionContextRepository.updateRegion(subscriptionId, region.region());
        jobScheduler.scheduleSubscriptionSync(customerId);
        return ResponseEntity.ok().build();
    }
    
}
