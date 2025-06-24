package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.model.resource.hetzner.HetznerNode;
import com.mc_host.api.model.resource.hetzner.HetznerSpec;
import com.mc_host.api.model.subscription.ContentSubscription;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.repository.PlanRepository;
import com.mc_host.api.repository.SubscriptionRepository;
import com.mc_host.api.service.resources.HetznerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CloudNodeStep extends AbstractStep {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final NodeRepository nodeRepository;
    private final HetznerService hetznerService;

    @Override
    public StepType getType() {
        return StepType.CLOUD_NODE;
    }

    @Override
    @Transactional
    public StepTransition create(Context context) {
        //TODO: some query for subscriptionId -> specificationId
        String priceId = subscriptionRepository.selectSubscription(context.getSubscriptionId())
            .map(ContentSubscription::priceId)
            .orElseThrow(() -> new IllegalStateException(String.format("Couldn't find subscription: %s", context.getSubscriptionId())));
        String specificationId = planRepository.selectSpecificationId(priceId)
            .orElseThrow(() -> new IllegalStateException(String.format("No specification could be found for price: %s", priceId)));
        HetznerSpec hetznerSpecification = HetznerSpec.fromSpecificationId(specificationId);
        HetznerNode hetznerNode = hetznerService.createCloudNode(context.getSubscriptionId(), hetznerSpecification);

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
