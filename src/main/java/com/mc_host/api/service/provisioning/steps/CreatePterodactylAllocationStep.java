package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.model.resource.dns.DnsARecord;
import com.mc_host.api.model.resource.pterodactyl.PterodactylNode;
import com.mc_host.api.repository.NodeAccessoryRepository;
import com.mc_host.api.service.resources.PterodactylService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreatePterodactylAllocationStep extends AbstractStep {

    private final NodeAccessoryRepository nodeAccessoryRepository;
    private final PterodactylService pterodactylService;

    @Override
    public StepType getType() {
        return StepType.CREATE_PTERODACTYL_ALLOCATION;
    }

    @Override
    @Transactional
    public StepTransition create(Context context) {
        //Skip for dedicated resources
        if (false) {
            LOGGER.warning("%s step is illegal for dedicated resources. Skipping. subId: %s".formatted(getType(), context.getSubscriptionId()));
            return transitionService.persistAndProgress(context, StepType.ASSIGN_PTERODACTYL_ALLOCATION);
        }

        PterodactylNode pterodactylNode = nodeAccessoryRepository.selectPterodactylNode(context.getNewPterodactylNodeId())
            .orElseThrow(() -> new IllegalStateException("Pterodactyl node not found: " + context.getNewPterodactylNodeId()));
        DnsARecord dnsARecord = nodeAccessoryRepository.selectDnsARecord(context.getNewARecordId())
            .orElseThrow(() -> new IllegalStateException("DNS A Record not found: " + context.getNewARecordId()));
        pterodactylService.createAllocation(pterodactylNode.pterodactylNodeId(), dnsARecord.content(), 25565);

        return transitionService.persistAndProgress(context, StepType.ASSIGN_PTERODACTYL_ALLOCATION);
    }

    @Override
    @Transactional
    public StepTransition destroy(Context context) {
        LOGGER.warning("%s step is illegal for destroys. Skipping. subId: %s".formatted(getType(), context.getSubscriptionId()));
        return transitionService.persistAndProgress(context, StepType.PTERODACTYL_NODE);
    }

}
