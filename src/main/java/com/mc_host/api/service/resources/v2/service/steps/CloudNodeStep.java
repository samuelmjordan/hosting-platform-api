package com.mc_host.api.service.resources.v2.service.steps;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.MarketingRegion;
import com.mc_host.api.model.MetadataKey;
import com.mc_host.api.model.hetzner.HetznerRegion;
import com.mc_host.api.model.hetzner.HetznerServerType;
import com.mc_host.api.model.node.HetznerNode;
import com.mc_host.api.model.node.Node;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.repository.SubscriptionRepository;
import com.mc_host.api.service.resources.HetznerService;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.StepTransition;
import com.mc_host.api.service.resources.v2.context.StepType;

@Service
public class CloudNodeStep extends AbstractStep {

    private final NodeRepository nodeRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final HetznerService hetznerService;

    protected CloudNodeStep(
        ServerExecutionContextRepository contextRepository,
        NodeRepository nodeRepository,
        SubscriptionRepository subscriptionRepository,
        HetznerService hetznerService
    ) {
        super(contextRepository);
        this.nodeRepository = nodeRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.hetznerService = hetznerService;
    }

    @Override
    public StepType getType() {
        return StepType.CLOUD_NODE;
    }

    @Override
    public StepTransition create(Context context) {
        HetznerRegion hetznerRegion = MarketingRegion.valueOf(
            subscriptionRepository.selectSubscriptionStripeMetadata(context.getSubscriptionId())
                .orElseThrow(() -> new IllegalStateException("Subscription not found: " + context.getSubscriptionId()))
                .getOrDefault(MetadataKey.REGION.name(), MarketingRegion.WEST_EUROPE.name())
            ).getHetznerRegion();

        HetznerNode hetznerNode = hetznerService.createCloudNode(context.getSubscriptionId(), hetznerRegion, HetznerServerType.CAX11);
        nodeRepository.insertHetznerNode(hetznerNode);

        return inProgress(context, StepType.A_RECORD);
    }

    @Override
    public StepTransition destroy(Context context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'destroy'");
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
