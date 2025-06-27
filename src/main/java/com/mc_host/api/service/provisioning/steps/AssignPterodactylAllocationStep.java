package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.model.resource.hetzner.node.HetznerNode;
import com.mc_host.api.model.resource.pterodactyl.PterodactylAllocation;
import com.mc_host.api.model.resource.pterodactyl.PterodactylNode;
import com.mc_host.api.repository.NodeAccessoryRepository;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.service.resources.PterodactylService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssignPterodactylAllocationStep extends AbstractStep {

    private final NodeRepository nodeRepository;
    private final NodeAccessoryRepository nodeAccessoryRepository;
    private final PterodactylService pterodactylService;

    @Override
    public StepType getType() {
        return StepType.ASSIGN_PTERODACTYL_ALLOCATION;
    }

    @Override
    @Transactional
    public StepTransition create(Context context) {
        PterodactylNode pterodactylNode = nodeAccessoryRepository.selectPterodactylNode(context.getNewPterodactylNodeId())
            .orElseThrow(() -> new IllegalStateException("Pterodactyl node not found: " + context.getNewPterodactylNodeId()));
        PterodactylAllocation pterodactylAllocation = pterodactylService.getAllocation(context.getSubscriptionId(), pterodactylNode.pterodactylNodeId());

        Context transitionedContext = context.withNewAllocationId(pterodactylAllocation.allocationId());
        nodeAccessoryRepository.insertPterodactylAllocation(pterodactylAllocation);

        return transitionService.persistAndProgress(transitionedContext, StepType.PTERODACTYL_SERVER);
    }

    @Override
    @Transactional
    public StepTransition destroy(Context context) {
        PterodactylAllocation pterodactylAllocation = nodeAccessoryRepository.selectPterodactylAllocation(context.getAllocationId())
            .orElseThrow(() -> new IllegalStateException("Pterodactyl allocation not found: " + context.getAllocationId()));
        Context transitionedContext = context.promoteNewAllocationId();
        nodeAccessoryRepository.deletePterodactylAllocation(pterodactylAllocation.allocationId());

        Boolean dedicated = nodeRepository.selectHetznerNode(context.getNodeId())
            .map(HetznerNode::dedicated)
            .orElseThrow(() -> new IllegalStateException(String.format("Node %s not found", context.getNodeId())));
        if (dedicated) {
            return transitionService.persistAndProgress(transitionedContext, StepType.TRY_ALLOCATE_DEDICATED_NODE);
        }
        return transitionService.persistAndProgress(transitionedContext, StepType.PTERODACTYL_NODE);
    }

}
