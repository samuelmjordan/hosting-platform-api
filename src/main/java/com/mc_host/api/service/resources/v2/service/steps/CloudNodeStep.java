package com.mc_host.api.service.resources.v2.service.steps;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.MarketingRegion;
import com.mc_host.api.model.entity.ContentSubscription;
import com.mc_host.api.model.hetzner.HetznerRegion;
import com.mc_host.api.model.hetzner.HetznerServerType;
import com.mc_host.api.model.node.HetznerNode;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.repository.SubscriptionRepository;
import com.mc_host.api.service.resources.HetznerService;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.StepTransition;
import com.mc_host.api.service.resources.v2.context.StepType;
import com.mc_host.api.service.resources.v2.service.TransitionService;

@Service
public class CloudNodeStep extends AbstractStep {

    private final NodeRepository nodeRepository;
    private final HetznerService hetznerService;

    protected CloudNodeStep(
        ServerExecutionContextRepository contextRepository,
        TransitionService transitionService,
        NodeRepository nodeRepository,
        HetznerService hetznerService
    ) {
        super(contextRepository, transitionService);
        this.nodeRepository = nodeRepository;
        this.hetznerService = hetznerService;
    }

    @Override
    public StepType getType() {
        return StepType.CLOUD_NODE;
    }

    @Override
    public StepTransition create(Context context) {
        HetznerNode hetznerNode = hetznerService.createCloudNode(context.getSubscriptionId(), context.getRegion().getHetznerRegion(), HetznerServerType.CAX11);
        nodeRepository.insertHetznerCloudNode(hetznerNode);

        return transitionService.persistAndProgress(context, StepType.A_RECORD);
    }

    @Override
    public StepTransition destroy(Context context) {
        HetznerNode hetznerNode = nodeRepository.selectHetznerNode(context.getSubscriptionId())
            .orElseThrow(() -> new IllegalStateException("Hetzner node not found for subscription: " + context.getSubscriptionId()));
        hetznerService.deleteCloudNode(hetznerNode.nodeId());

        return transitionService.persistAndProgress(context, StepType.ALLOCATE_NODE);
    }

}
