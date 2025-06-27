package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.model.resource.hetzner.node.HetznerNode;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.NodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SyncNodeRouteStep extends AbstractStep {

    private final NodeRepository nodeRepository;
    private final GameServerRepository gameServerRepository;

    @Override
    public StepType getType() {
        return StepType.SYNC_NODE_ROUTE;
    }

    @Override
    @Transactional
    public StepTransition create(Context context) {
        //Skip for cloud resources
        Boolean dedicated = nodeRepository.selectHetznerNode(context.getNewNodeId())
            .map(HetznerNode::dedicated)
            .orElseThrow(() -> new IllegalStateException(String.format("Node %s not found", context.getNewNodeId())));
        if (!dedicated) {
            LOGGER.warning("%s step is illegal for cloud resources. Skipping. subId: %s".formatted(getType(), context.getSubscriptionId()));
            return transitionService.persistAndProgress(context, StepType.FINALISE);
        }

        //do

        //Early return for non-migrations
        if (!context.getMode().isMigrate()) {
            return transitionService.persistAndProgress(context, StepType.FINALISE);
        }

        return transitionService.persistAndProgress(context, StepType.READY);
    }

    @Override
    @Transactional
    public StepTransition destroy(Context context) {
        LOGGER.warning("%s step is illegal for destroys. Skipping. subId: %s".formatted(getType(), context.getSubscriptionId()));
        return transitionService.persistAndProgress(context, StepType.C_NAME_RECORD);
    }

}
