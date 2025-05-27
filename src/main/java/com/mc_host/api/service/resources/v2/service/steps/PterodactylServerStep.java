package com.mc_host.api.service.resources.v2.service.steps;

import org.springframework.stereotype.Service;

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
import com.stripe.model.tax.Registration.CountryOptions.Pt;

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
    public StepTransition create(Context context) {
        PterodactylAllocation allocationAttributes = nodeRepository.selectPterodactylAllocation(context.getSubscriptionId())
            .orElseThrow(() -> new IllegalStateException("Pterodactyl allocation not found for subscription: " + context.getSubscriptionId()));
        PterodactylServer pterodactylServer = pterodactylService.createServer(context.getSubscriptionId(), allocationAttributes);
        gameServerRepository.insertPterodactylServer(pterodactylServer);

        return transitionService.persistAndProgress(context, StepType.C_NAME_RECORD);
    }

    @Override
    public StepTransition destroy(Context context) {
        PterodactylServer pterodactylServer = gameServerRepository.selectPterodactylServer(context.getSubscriptionId())
            .orElseThrow(() -> new IllegalStateException("Pterodactyl server not found for subscription: " + context.getSubscriptionId()));
        pterodactylService.destroyServer(pterodactylServer.pterodactylServerId());

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
