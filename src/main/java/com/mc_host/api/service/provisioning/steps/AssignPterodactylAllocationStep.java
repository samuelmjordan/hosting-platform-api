package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.model.resource.dns.DnsARecord;
import com.mc_host.api.model.resource.pterodactyl.PterodactylAllocation;
import com.mc_host.api.model.resource.pterodactyl.PterodactylNode;
import com.mc_host.api.repository.NodeAccessoryRepository;
import com.mc_host.api.service.resources.PterodactylService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssignPterodactylAllocationStep extends AbstractStep {

    private final NodeAccessoryRepository nodeAccessoryRepository;
    private final PterodactylService pterodactylService;

    @Override
    public StepType getType() {
        return StepType.ASSIGN_PTERODACTYL_ALLOCATION;
    }

    @Override
    @Transactional
    public StepTransition create(Context context) {
        PterodactylNode pterodactylNode = nodeAccessoryRepository.selectPterodactylNode(context.getNewPterodactylNodeId())
            .orElseThrow(() -> new IllegalStateException("Pterodactyl node not found: " + context.getNewPterodactylNodeId()));
        DnsARecord dnsARecord = nodeAccessoryRepository.selectDnsARecord(context.getNewARecordId())
            .orElseThrow(() -> new IllegalStateException("DNS A Record not found: " + context.getNewARecordId()));
        PterodactylAllocation pterodactylAllocation = pterodactylService.getAllocation(context.getSubscriptionId(), pterodactylNode.pterodactylNodeId());

        Context transitionedContext = context.withNewAllocationId(pterodactylAllocation.allocationId());
        nodeAccessoryRepository.insertPterodactylAllocation(pterodactylAllocation);

        return transitionService.persistAndProgress(transitionedContext, StepType.PTERODACTYL_SERVER);
    }

    @Override
    @Transactional
    @SuppressWarnings("unused")
    public StepTransition destroy(Context context) {
        PterodactylAllocation pterodactylAllocation = nodeAccessoryRepository.selectPterodactylAllocation(context.getAllocationId())
            .orElseThrow(() -> new IllegalStateException("Pterodactyl allocation not found: " + context.getAllocationId()));
        Context transitionedContext = context.promoteNewAllocationId();
        nodeAccessoryRepository.deletePterodactylAllocation(pterodactylAllocation.allocationId());

        // TODO: identify is server is hosted on a dedicated or cloud node
        // For now, we assume that the server is hosted on a cloud node
        if (false) {
            return transitionService.persistAndProgress(transitionedContext, StepType.TRY_ALLOCATE_DEDICATED_NODE);
        }
        return transitionService.persistAndProgress(transitionedContext, StepType.PTERODACTYL_NODE);
    }

}
