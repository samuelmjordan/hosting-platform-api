package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.plan.ServerSpecification;
import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.model.resource.hetzner.HetznerCloudProduct;
import com.mc_host.api.model.resource.hetzner.node.HetznerClaim;
import com.mc_host.api.model.resource.hetzner.node.HetznerCloudNode;
import com.mc_host.api.model.resource.hetzner.node.HetznerNode;
import com.mc_host.api.model.subscription.ContentSubscription;
import com.mc_host.api.repository.GameServerSpecRepository;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.repository.PlanRepository;
import com.mc_host.api.repository.SubscriptionRepository;
import com.mc_host.api.service.resources.HetznerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AllocateCloudNodeStep extends AbstractStep {

    private final SubscriptionRepository subscriptionRepository;
    private final GameServerSpecRepository gameServerSpecRepository;
    private final PlanRepository planRepository;
    private final NodeRepository nodeRepository;
    private final HetznerService hetznerService;

    @Override
    public StepType getType() {
        return StepType.ALLOCATE_CLOUD_NODE;
    }

    @Override
    @Transactional
    public StepTransition create(Context context) {
        //Skip for dedicated resources
        //Cloud nodes havent been created yet
        Boolean dedicated = nodeRepository.selectHetznerNode(context.getNewNodeId())
            .map(HetznerNode::dedicated)
            .orElse(false);
        if (dedicated) {
            LOGGER.warning("%s step is illegal for dedicated resources. Skipping. subId: %s".formatted(getType(), context.getSubscriptionId()));
            return transitionService.persistAndProgress(context, StepType.ASSIGN_PTERODACTYL_ALLOCATION);
        }

        //TODO: some query for subscriptionId -> specificationId
        String priceId = subscriptionRepository.selectSubscription(context.getSubscriptionId())
            .map(ContentSubscription::priceId)
            .orElseThrow(() -> new IllegalStateException(String.format("Couldn't find subscription: %s", context.getSubscriptionId())));
        String specificationId = planRepository.selectSpecificationId(priceId)
            .orElseThrow(() -> new IllegalStateException(String.format("No specification could be found for price: %s", priceId)));
        HetznerCloudProduct hetznerSpecification = HetznerCloudProduct.fromSpecificationId(specificationId);
        HetznerCloudNode hetznerCloudNode = hetznerService.createCloudNode(context.getSubscriptionId(), hetznerSpecification);

        Long specRam = gameServerSpecRepository.selectSpecification(specificationId)
            .map(ServerSpecification::ram_gb)
            .map(Long::valueOf)
            .orElseThrow(() -> new IllegalStateException(String.format("No specification could be found for price: %s", priceId)));
        nodeRepository.insertCloudNode(hetznerCloudNode);
        nodeRepository.insertClaim(new HetznerClaim(
            context.getSubscriptionId(),
            hetznerCloudNode.hetznerNodeId(),
            specRam
        ));
        Context transitionedContext = context.withNewNodeId(hetznerCloudNode.hetznerNodeId());

        return transitionService.persistAndProgress(transitionedContext, StepType.NODE_A_RECORD);
    }

    @Override
    @Transactional
    public StepTransition destroy(Context context) {
        //Skip for dedicated resources
        Boolean dedicated = nodeRepository.selectHetznerNode(context.getNodeId())
            .map(HetznerNode::dedicated)
            .orElseThrow(() -> new IllegalStateException(String.format("Node %s not found", context.getNodeId())));
        if (dedicated) {
            LOGGER.warning("%s step is illegal for dedicated resources. Skipping. subId: %s".formatted(getType(), context.getSubscriptionId()));
            return transitionService.persistAndProgress(context, StepType.TRY_ALLOCATE_DEDICATED_NODE);
        }

        Long hetznerNodeId = nodeRepository.selectHetznerCloudNode(context.getNodeId())
            .map(HetznerCloudNode::hetznerNodeId)
            .orElseThrow(() -> new IllegalStateException("Hetzner node not found for subscription: " + context.getSubscriptionId()));
        Context transitionedContext = context.promoteNewNodeId();
        nodeRepository.deleteClaim(context.getSubscriptionId(), hetznerNodeId);
        nodeRepository.deleteHetznerCloudNode(hetznerNodeId);
        
        hetznerService.deleteCloudNode(hetznerNodeId);

        return transitionService.persistAndProgress(transitionedContext, StepType.NEW);
    }

}
