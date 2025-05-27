package com.mc_host.api.service.resources.v2.service.steps;

import com.mc_host.api.model.node.DnsARecord;
import com.mc_host.api.model.node.PterodactylAllocation;
import com.mc_host.api.model.node.PterodactylNode;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.resources.PterodactylService;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.StepTransition;
import com.mc_host.api.service.resources.v2.context.StepType;
import com.mc_host.api.service.resources.v2.service.TransitionService;

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
    public StepTransition create(Context context) {
        PterodactylNode pterodactylNode = nodeRepository.selectPterodactylNode(context.getSubscriptionId())
            .orElseThrow(() -> new IllegalStateException("Pterodactyl node not found for subscription: " + context.getSubscriptionId()));
        DnsARecord dnsARecord = nodeRepository.selectDnsARecord(context.getSubscriptionId())
            .orElseThrow(() -> new IllegalStateException("DNS A Record not found for subscription: " + context.getSubscriptionId()));
        pterodactylService.createAllocation(pterodactylNode.pterodactylNodeId(), dnsARecord.content(), 25565);
        PterodactylAllocation pterodactylAllocation = pterodactylService.getAllocation(pterodactylNode.pterodactylNodeId());
        nodeRepository.insertPterodactylAllocation(pterodactylAllocation);

        return transitionService.persistAndProgress(context, StepType.PTERODACTYL_SERVER);
    }

    @SuppressWarnings("unused")
    @Override
    public StepTransition destroy(Context context) {
        // TODO: identify is server is hosted on a dedicated or cloud node
        // For now, we assume that the server is hosted on a cloud node
        if (false) {
            return transitionService.persistAndProgress(context, StepType.DEDICATED_NODE);
        }
        return transitionService.persistAndProgress(context, StepType.PTERODACTYL_NODE);
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
