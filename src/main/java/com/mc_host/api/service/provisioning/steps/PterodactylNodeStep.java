package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.plan.ServerSpecification;
import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.model.resource.dns.DnsARecord;
import com.mc_host.api.model.resource.hetzner.node.HetznerNode;
import com.mc_host.api.model.resource.pterodactyl.PterodactylNode;
import com.mc_host.api.model.subscription.ContentSubscription;
import com.mc_host.api.repository.GameServerSpecRepository;
import com.mc_host.api.repository.NodeAccessoryRepository;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.repository.PlanRepository;
import com.mc_host.api.repository.SubscriptionRepository;
import com.mc_host.api.service.resources.PterodactylService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PterodactylNodeStep extends AbstractStep {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final NodeRepository nodeRepository;
    private final NodeAccessoryRepository nodeAccessoryRepository;
    private final GameServerSpecRepository gameServerSpecRepository;
    private final PterodactylService pterodactylService;

    @Override
    public StepType getType() {
        return StepType.PTERODACTYL_NODE;
    }

    @Override
    @Transactional
    public StepTransition create(Context context) {
        //Skip for dedicated resources
        Boolean dedicated = nodeRepository.selectHetznerNode(context.getNewNodeId())
            .map(HetznerNode::dedicated)
            .orElseThrow(() -> new IllegalStateException(String.format("Node %s not found", context.getNewNodeId())));
        if (dedicated) {
            LOGGER.warning("%s step is illegal for dedicated resources. Skipping. subId: %s".formatted(getType(), context.getSubscriptionId()));
            return transitionService.persistAndProgress(context, StepType.ASSIGN_PTERODACTYL_ALLOCATION);
        }

        String priceId = subscriptionRepository.selectSubscription(context.getSubscriptionId())
            .map(ContentSubscription::priceId)
            .orElseThrow(() -> new IllegalStateException(String.format("Couldn't find subscription: %s", context.getSubscriptionId())));
        String specificationId = planRepository.selectSpecificationId(priceId)
            .orElseThrow(() -> new IllegalStateException(String.format("No specification could be found for price: %s", priceId)));
        DnsARecord dnsARecord = nodeAccessoryRepository.selectDnsARecord(context.getNewARecordId())
            .orElseThrow(() -> new IllegalStateException("DNS A Record not found:" + context.getNewARecordId()));
        ServerSpecification serverSpecification = gameServerSpecRepository.selectSpecification(specificationId)
            .orElseThrow(() -> new IllegalStateException("Specification not found:" + specificationId));
        PterodactylNode pterodactylNode = pterodactylService.createNode(dnsARecord, serverSpecification);

        Context transitionedContext = context.withNewPterodactylNodeId(pterodactylNode.pterodactylNodeId());
        nodeAccessoryRepository.insertPterodactylNode(pterodactylNode);

        return transitionService.persistAndProgress(transitionedContext, StepType.CONFIGURE_NODE);
    }

    @Override
    @Transactional
    public StepTransition destroy(Context context) {
        //Skip for dedicated resources
        Boolean dedicated = nodeRepository.selectHetznerNode(context.getNodeId())
            .map(HetznerNode::dedicated)
            .orElseThrow(() -> new IllegalStateException(String.format("Node %s not found", context.getNodeId())));
        if (dedicated) {
            LOGGER.warning("%s step is illegal for dedicated resources. Skipping. subId: %s".formatted(getType(), context.getSubscriptionId()));
            return transitionService.persistAndProgress(context, StepType.TRY_ALLOCATE_DEDICATED_NODE);
        }

        PterodactylNode pterodactylNode = nodeAccessoryRepository.selectPterodactylNode(context.getPterodactylNodeId())
            .orElseThrow(() -> new IllegalStateException("Pterodactyl Node not found: " + context.getPterodactylNodeId()));
        Context transitionedContext = context.promoteNewPterodactylNodeId();
        nodeAccessoryRepository.deletePterodactylNode(pterodactylNode.pterodactylNodeId());

        pterodactylService.destroyNode(pterodactylNode.pterodactylNodeId());

        return transitionService.persistAndProgress(transitionedContext, StepType.NODE_A_RECORD);
    }

}
