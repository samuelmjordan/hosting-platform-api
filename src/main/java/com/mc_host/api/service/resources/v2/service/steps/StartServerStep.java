package com.mc_host.api.service.resources.v2.service.steps;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.resource.pterodactyl.PterodactylServer;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.resources.PterodactylService;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.StepTransition;
import com.mc_host.api.service.resources.v2.context.StepType;
import com.mc_host.api.service.resources.v2.service.TransitionService;

@Service
public class StartServerStep extends AbstractStep {

    private final GameServerRepository gameServerRepository;
    private final PterodactylService pterodactylService;

    protected StartServerStep(
        ServerExecutionContextRepository contextRepository,
        GameServerRepository gameServerRepository,
        TransitionService transitionService,
        PterodactylService pterodactylService
    ) {
        super(contextRepository, transitionService);
        this.gameServerRepository = gameServerRepository;
        this.pterodactylService = pterodactylService;
    }

    @Override
    public StepType getType() {
        return StepType.START_SERVER;
    }

    @Override
    public StepTransition create(Context context) {
        PterodactylServer pterodactylServer = gameServerRepository.selectPterodactylServer(context.getNewPterodactylServerId())
            .orElseThrow(() -> new IllegalStateException("Pterodactyl server not found: " + context.getNewPterodactylServerId()));
        pterodactylService.startNewPterodactylServer(pterodactylServer);
        
        return transitionService.persistAndProgress(context, StepType.FINALISE);
    }

    @Override
    public StepTransition destroy(Context context) {
        return transitionService.persistAndProgress(context, StepType.PTERODACTYL_SERVER);
    }
    
}
