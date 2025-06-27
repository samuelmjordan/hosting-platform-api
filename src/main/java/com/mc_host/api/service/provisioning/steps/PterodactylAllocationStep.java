package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.model.resource.dns.DnsARecord;
import com.mc_host.api.model.resource.pterodactyl.PterodactylAllocation;
import com.mc_host.api.model.resource.pterodactyl.PterodactylNode;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.service.resources.PterodactylService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PterodactylAllocationStep extends AbstractStep {

    private final NodeRepository nodeRepository;
    private final PterodactylService pterodactylService;

    @Override
    public StepType getType() {
        return StepType.PTERODACTYL_ALLOCATION;
    }

    @Override
    @Transactional
    public StepTransition create(Context context) {
        PterodactylNode pterodactylNode = nodeRepository.selectPterodactylNode(context.getNewPterodactylNodeId())
            .orElseThrow(() -> new IllegalStateException("Pterodactyl node not found: " + context.getNewPterodactylNodeId()));
        DnsARecord dnsARecord = nodeRepository.selectDnsARecord(context.getNewARecordId())
            .orElseThrow(() -> new IllegalStateException("DNS A Record not found: " + context.getNewARecordId()));
        pterodactylService.createDefaultAllocationRange(pterodactylNode.pterodactylNodeId(), dnsARecord.content());
        PterodactylAllocation pterodactylAllocation = pterodactylService.getAllocation(context.getSubscriptionId(), pterodactylNode.pterodactylNodeId());

        Context transitionedContext = context.withNewAllocationId(pterodactylAllocation.allocationId());
        nodeRepository.insertPterodactylAllocation(pterodactylAllocation);

        return transitionService.persistAndProgress(transitionedContext, StepType.PTERODACTYL_SERVER);
    }

    @Override
    @Transactional
    @SuppressWarnings("unused")
    public StepTransition destroy(Context context) {
        // TODO: identify is server is hosted on a dedicated or cloud node
        // For now, we assume that the server is hosted on a cloud node
        PterodactylAllocation pterodactylAllocation = nodeRepository.selectPterodactylAllocation(context.getAllocationId())
            .orElseThrow(() -> new IllegalStateException("Pterodactyl allocation not found: " + context.getAllocationId()));
        Context transitionedContext = context.promoteNewAllocationId();
        nodeRepository.deletePterodactylAllocation(pterodactylAllocation.allocationId());
        if (false) {
            return transitionService.persistAndProgress(transitionedContext, StepType.DEDICATED_NODE);
        }
        return transitionService.persistAndProgress(transitionedContext, StepType.PTERODACTYL_NODE);
    }

}
