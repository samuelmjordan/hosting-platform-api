package com.mc_host.api.service.resources.v2.service.steps;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mc_host.api.model.game_server.PterodactylServer;
import com.mc_host.api.model.node.PterodactylAllocation;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.resources.PterodactylService;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.StepTransition;
import com.mc_host.api.service.resources.v2.context.StepType;
import com.mc_host.api.service.resources.v2.service.TransitionService;

@Service
public class PterodactylServerStep extends AbstractStep {

    private final NodeRepository nodeRepository;
    private final GameServerRepository gameServerRepository;
    private final PterodactylService pterodactylService;

    protected PterodactylServerStep(
        ServerExecutionContextRepository contextRepository,
        GameServerRepository gameServerRepository,
        TransitionService transitionService,
        NodeRepository nodeRepository,
        PterodactylService pterodactylService
    ) {
        super(contextRepository, transitionService);
        this.nodeRepository = nodeRepository;
        this.gameServerRepository = gameServerRepository;
        this.pterodactylService = pterodactylService;
    }

    @Override
    public StepType getType() {
        return StepType.PTERODACTYL_SERVER;
    }

    @Override
    @Transactional
    public StepTransition create(Context context) {
        PterodactylAllocation allocationAttributes = nodeRepository.selectPterodactylAllocation(context.getNewAllocationId())
            .orElseThrow(() -> new IllegalStateException("Pterodactyl allocation not found: " + context));
        PterodactylServer pterodactylServer = pterodactylService.createServer(context.getSubscriptionId(), allocationAttributes);

        Context transitionedContext = context.withNewAllocationId(pterodactylServer.pterodactylServerId());
        gameServerRepository.insertPterodactylServer(pterodactylServer);

        if (context.getMode().isMigrate()) {
            return transitionService.persistAndProgress(transitionedContext, StepType.START_SERVER);
        }
        return transitionService.persistAndProgress(transitionedContext, StepType.C_NAME_RECORD);
    }

    @Override
    @Transactional
    public StepTransition destroy(Context context) {
        PterodactylServer pterodactylServer = gameServerRepository.selectPterodactylServer(context.getPterodactylServerId())
            .orElseThrow(() -> new IllegalStateException("Pterodactyl server not found for subscription: " + context.getSubscriptionId()));
        Context transitionedContext = context.promoteNewPterodactylNodeId();
        gameServerRepository.deletePterodactylServer(pterodactylServer.pterodactylServerId());

        pterodactylService.destroyServer(pterodactylServer.pterodactylServerId());

        return transitionService.persistAndProgress(transitionedContext, StepType.PTERODACTYL_NODE);
    }

}
