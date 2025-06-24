package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.model.resource.pterodactyl.PterodactylServer;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.service.resources.PterodactylService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StartServerStep extends AbstractStep {

    private final GameServerRepository gameServerRepository;
    private final PterodactylService pterodactylService;

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
