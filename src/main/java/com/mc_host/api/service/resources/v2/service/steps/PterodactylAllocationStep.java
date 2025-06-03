package com.mc_host.api.service.resources.v2.service.steps;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mc_host.api.model.resource.DnsARecord;
import com.mc_host.api.model.resource.pterodactyl.PterodactylAllocation;
import com.mc_host.api.model.resource.pterodactyl.PterodactylNode;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.resources.PterodactylService;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.StepTransition;
import com.mc_host.api.service.resources.v2.context.StepType;
import com.mc_host.api.service.resources.v2.service.TransitionService;

@Service
public class PterodactylAllocationStep extends AbstractStep {

    private final NodeRepository nodeRepository;
    private final PterodactylService pterodactylService;

    protected PterodactylAllocationStep(
        ServerExecutionContextRepository contextRepository,
        TransitionService transitionService,
        NodeRepository nodeRepository,
        PterodactylService pterodactylService
    ) {
        super(contextRepository, transitionService);
        this.nodeRepository = nodeRepository;
        this.pterodactylService = pterodactylService;
    }

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
        pterodactylService.createAllocation(pterodactylNode.pterodactylNodeId(), dnsARecord.content(), 25565);
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
