package com.mc_host.api.service.resources.v2.service.steps;

import org.springframework.stereotype.Service;

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
public class ConfigureNodeStep extends AbstractStep {

    private final NodeRepository nodeRepository;
    private final PterodactylService pterodactylService;

    protected ConfigureNodeStep(
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
        return StepType.CONFIGURE_NODE;
    }

    @Override
    public StepTransition create(Context context) {
        PterodactylNode pterodactylNode = nodeRepository.selectPterodactylNode(context.getSubscriptionId())
            .orElseThrow(() -> new IllegalStateException("Pterodactyl node not found for subscription: " + context.getSubscriptionId()));
        DnsARecord dnsARecord = nodeRepository.selectDnsARecord(context.getSubscriptionId())
            .orElseThrow(() -> new IllegalStateException("DNS A Record not found for subscription: " + context.getSubscriptionId()));
        pterodactylService.configureNode(pterodactylNode.pterodactylNodeId(), dnsARecord);

        return transitionService.persistAndProgress(context, StepType.PTERODACTYL_ALLOCATION);
    }

    @Override
    public StepTransition destroy(Context context) {
        throw new UnsupportedOperationException("Node configuration step cannot be destroyed directly. Try destroying the Pterodactyl Node step instead.");
    }

}
