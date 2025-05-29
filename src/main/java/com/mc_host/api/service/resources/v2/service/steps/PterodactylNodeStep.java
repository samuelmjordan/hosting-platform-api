package com.mc_host.api.service.resources.v2.service.steps;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mc_host.api.model.node.DnsARecord;
import com.mc_host.api.model.node.PterodactylNode;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.resources.PterodactylService;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.StepTransition;
import com.mc_host.api.service.resources.v2.context.StepType;
import com.mc_host.api.service.resources.v2.service.TransitionService;

@Service
public class PterodactylNodeStep extends AbstractStep {

    private final NodeRepository nodeRepository;
    private final PterodactylService pterodactylService;

    protected PterodactylNodeStep(
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
        return StepType.PTERODACTYL_NODE;
    }

    @Override
    @Transactional
    public StepTransition create(Context context) {
        DnsARecord dnsARecord = nodeRepository.selectDnsARecord(context.getNewARecordId())
            .orElseThrow(() -> new IllegalStateException("DNS A Record not found:" + context.getNewARecordId()));
        PterodactylNode pterodactylNode = pterodactylService.createNode(dnsARecord);

        Context transitionedContext = context.withNewPterodactylNodeId(pterodactylNode.pterodactylNodeId());
        nodeRepository.insertPterodactylNode(pterodactylNode);

        return transitionService.persistAndProgress(transitionedContext, StepType.CONFIGURE_NODE);
    }

    @Override
    @Transactional
    public StepTransition destroy(Context context) {
        PterodactylNode pterodactylNode = nodeRepository.selectPterodactylNode(context.getPterodactylNodeId())
            .orElseThrow(() -> new IllegalStateException("Pterodactyl Node not found for subscription: " + context.getSubscriptionId()));
        Context transitionedContext = context.promoteNewPterodactylNodeId();
        nodeRepository.deletePterodactylNode(pterodactylNode.pterodactylNodeId());

        pterodactylService.destroyNode(pterodactylNode.pterodactylNodeId());

        return transitionService.persistAndProgress(transitionedContext, StepType.A_RECORD);
    }

}
