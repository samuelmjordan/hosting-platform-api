package com.mc_host.api.service.resources.v2.service.steps;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mc_host.api.model.resource.hetzner.HetznerNode;
import com.mc_host.api.model.resource.hetzner.HetznerServerType;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
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
    @Transactional
    public StepTransition create(Context context) {
        HetznerNode hetznerNode = hetznerService.createCloudNode(context.getSubscriptionId(), context.getRegion().getHetznerRegion(), HetznerServerType.CAX11);

        Context transitionedContext = context.withNewNodeId(hetznerNode.nodeId());
        nodeRepository.insertHetznerCloudNode(hetznerNode);

        return transitionService.persistAndProgress(transitionedContext, StepType.A_RECORD);
    }

    @Override
    @Transactional
    public StepTransition destroy(Context context) {
        HetznerNode hetznerNode = nodeRepository.selectHetznerNode(context.getNodeId())
            .orElseThrow(() -> new IllegalStateException("Hetzner node not found for subscription: " + context.getSubscriptionId()));
        Context transitionedContext = context.promoteNewNodeId();
        nodeRepository.deleteHetznerNode(hetznerNode.nodeId());
        
        hetznerService.deleteCloudNode(hetznerNode.nodeId());

        return transitionService.persistAndProgress(transitionedContext, StepType.ALLOCATE_NODE);
    }

}
