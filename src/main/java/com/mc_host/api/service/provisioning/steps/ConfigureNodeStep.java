package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.model.resource.dns.DnsARecord;
import com.mc_host.api.model.resource.hetzner.node.HetznerNode;
import com.mc_host.api.model.resource.pterodactyl.PterodactylNode;
import com.mc_host.api.repository.NodeAccessoryRepository;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.service.resources.PterodactylService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfigureNodeStep extends AbstractStep {

    private final NodeRepository nodeRepository;
    private final NodeAccessoryRepository nodeAccessoryRepository;
    private final PterodactylService pterodactylService;

    @Override
    public StepType getType() {
        return StepType.CONFIGURE_NODE;
    }

    @Override
    public StepTransition create(Context context) {
        //Skip for dedicated resources
        Boolean dedicated = nodeRepository.selectHetznerNode(context.getNewNodeId())
            .map(HetznerNode::dedicated)
            .orElseThrow(() -> new IllegalStateException(String.format("Node %s not found", context.getNewNodeId())));
        if (dedicated) {
            LOGGER.warning("%s step is illegal for dedicated resources. Skipping. subId: %s".formatted(getType(), context.getSubscriptionId()));
            return transitionService.persistAndProgress(context, StepType.ASSIGN_PTERODACTYL_ALLOCATION);
        }

        PterodactylNode pterodactylNode = nodeAccessoryRepository.selectPterodactylNode(context.getNewPterodactylNodeId())
            .orElseThrow(() -> new IllegalStateException("Pterodactyl node not found: " + context.getNewPterodactylNodeId()));
        DnsARecord dnsARecord = nodeAccessoryRepository.selectDnsARecord(context.getNewARecordId())
            .orElseThrow(() -> new IllegalStateException("DNS A Record not found: " + context.getNewARecordId()));
        pterodactylService.configureNode(pterodactylNode.pterodactylNodeId(), dnsARecord);

        return transitionService.persistAndProgress(context, StepType.CREATE_PTERODACTYL_ALLOCATION);
    }

    @Override
    public StepTransition destroy(Context context) {
        LOGGER.warning("%s step is illegal for destroys. Skipping. subId: %s".formatted(getType(), context.getSubscriptionId()));
        return transitionService.persistAndProgress(context, StepType.PTERODACTYL_NODE);
    }

}
