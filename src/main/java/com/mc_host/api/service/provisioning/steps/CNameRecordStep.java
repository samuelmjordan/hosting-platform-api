package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.model.resource.dns.DnsARecord;
import com.mc_host.api.model.resource.dns.DnsCNameRecord;
import com.mc_host.api.model.resource.hetzner.node.HetznerNode;
import com.mc_host.api.model.subscription.ContentSubscription;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.NodeAccessoryRepository;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.repository.SubscriptionRepository;
import com.mc_host.api.service.resources.DnsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CNameRecordStep extends AbstractStep {

    private final NodeRepository nodeRepository;
    private final NodeAccessoryRepository nodeAccessoryRepository;
    private final GameServerRepository gameServerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final DnsService dnsService;

    @Override
    public StepType getType() {
        return StepType.C_NAME_RECORD;
    }

    @Override
    @Transactional
    public StepTransition create(Context context) {
        //Skip for migrations
        if (context.getMode().isMigrate()) {
            LOGGER.warning("%s step is illegal for migrating. Skipping. subId: %s".formatted(getType(), context.getSubscriptionId()));
            return transitionService.persistAndProgress(context, StepType.SYNC_NODE_ROUTE);
        }

        DnsARecord dnsARecord = nodeAccessoryRepository.selectDnsARecord(context.getNewARecordId())
            .orElseThrow(() -> new IllegalStateException("DNS A record not found: " + context.getNewARecordId()));
        String subdomain = subscriptionRepository.selectSubscription(context.getSubscriptionId())
            .map(ContentSubscription::subdomain)
            .orElseThrow(() -> new IllegalStateException("Subscription not found: " + context.getSubscriptionId()));
        DnsCNameRecord dnsCNameRecord = dnsService.createCNameRecord(dnsARecord, subdomain);

        Context transitionedContext = context.withNewCNameRecordId(dnsCNameRecord.cNameRecordId());
        gameServerRepository.insertDnsCNameRecord(dnsCNameRecord);

        Boolean dedicated = nodeRepository.selectHetznerNode(context.getNewNodeId())
            .map(HetznerNode::dedicated)
            .orElseThrow(() -> new IllegalStateException(String.format("Node %s not found", context.getNewNodeId())));
        if (dedicated) {
            return transitionService.persistAndProgress(transitionedContext, StepType.SYNC_NODE_ROUTE);
        }

        return transitionService.persistAndProgress(transitionedContext, StepType.FINALISE);
    }

    @Override
    @Transactional
    public StepTransition destroy(Context context) {
        DnsCNameRecord dnsCNameRecord = gameServerRepository.selectDnsCNameRecord(context.getCNameRecordId())
            .orElseThrow(() -> new IllegalStateException("DNS CNAME record not found: " + context.getCNameRecordId()));

        //Redirect cname record for migrations
        //Destroy for non migrations
        Context transitionedContext = context;
        if (context.getMode().isMigrate()) {
            DnsARecord dnsARecord = nodeAccessoryRepository.selectDnsARecord(context.getNewARecordId())
                .orElseThrow(() -> new IllegalStateException("DNS A record not found: " + context.getNewARecordId()));
            DnsCNameRecord newDnsCNameRecord = dnsService.redirectCNameRecord(dnsARecord, dnsCNameRecord);

            gameServerRepository.updateDnsCNameRecord(newDnsCNameRecord);
        } else {
            transitionedContext = context.promoteNewCNameRecordId();
            gameServerRepository.deleteDnsCNameRecord(dnsCNameRecord.cNameRecordId());
            
            dnsService.deleteCNameRecord(dnsCNameRecord);
        }

        return transitionService.persistAndProgress(transitionedContext, StepType.PTERODACTYL_SERVER);
    }

}
