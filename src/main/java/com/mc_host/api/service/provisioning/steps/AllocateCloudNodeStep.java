package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.plan.ServerSpecification;
import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.model.resource.hetzner.HetznerCloudProduct;
import com.mc_host.api.model.resource.hetzner.HetznerRegion;
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
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.logging.Level;
import java.util.stream.Stream;

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

        //Sometimes hetzner api 412s
        //We try all equivalent available resources to try and avoid this
        List<Pair<HetznerRegion, HetznerCloudProduct>> regionProductMatrix = Stream.of(HetznerRegion.values())
            .flatMap(region -> Stream.of(HetznerCloudProduct.values())
                .filter(product -> product.getSpecificationId().equals(specificationId))
                .map(product -> Pair.of(region, product))
            )
            .toList();

        HetznerCloudNode hetznerCloudNode = null;
        for (Pair<HetznerRegion, HetznerCloudProduct> regionProductPair : regionProductMatrix) {
            try {
                hetznerCloudNode = hetznerService.createCloudNode(
                    context.getSubscriptionId(),
                    regionProductPair.getRight(),
                    regionProductPair.getLeft());
                break;
            } catch (Exception e) {
                LOGGER.log(
                    Level.SEVERE,
                    "Error provisioning hetzner cloud node for subscription %s".formatted(context.getSubscriptionId()),
                    e
                );
            }
        }

        if (hetznerCloudNode == null) {
            throw new RuntimeException("FATAL: Error provisioning hetzner cloud node for subscription %s".formatted(context.getSubscriptionId()));
        }

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
