package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.model.resource.pterodactyl.PterodactylServer;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.provisioning.TransitionService;
import com.mc_host.api.service.resources.PterodactylService;
import org.springframework.stereotype.Service;

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
