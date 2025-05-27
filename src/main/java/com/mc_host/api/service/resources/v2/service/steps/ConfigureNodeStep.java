package com.mc_host.api.service.resources.v2.service.steps;

import com.mc_host.api.model.node.DnsARecord;
import com.mc_host.api.model.node.PterodactylNode;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.resources.PterodactylService;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.StepTransition;
import com.mc_host.api.service.resources.v2.context.StepType;

public class ConfigureNodeStep extends AbstractStep {

    private final NodeRepository nodeRepository;
    private final PterodactylService pterodactylService;

    protected ConfigureNodeStep(
        ServerExecutionContextRepository contextRepository,
        NodeRepository nodeRepository,
        PterodactylService pterodactylService
    ) {
        super(contextRepository);
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

        return inProgress(context, StepType.PTERODACTYL_ALLOCATION);
    }

    @Override
    public StepTransition destroy(Context context) {
        throw new UnsupportedOperationException("Node configuration step cannot be destroyed directly. Try destroying the Pterodactyl Node step instead.");
    }

    @Override
    public StepTransition migrate(Context context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'migrate'");
    }

    @Override
    public StepTransition update(Context context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'update'");
    }

}
