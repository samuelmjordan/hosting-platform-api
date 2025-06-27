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

        if (context.getMode().isMigrate()) {
            return transitionService.persistAndProgress(context, StepType.SYNC_NODE_ROUTE);
        }
        return transitionService.persistAndProgress(context, StepType.C_NAME_RECORD);
    }

    @Override
    public StepTransition destroy(Context context) {
        LOGGER.warning("%s step is illegal for destroys. Skipping. subId: %s".formatted(getType(), context.getSubscriptionId()));
        return transitionService.persistAndProgress(context, StepType.PTERODACTYL_SERVER);
    }
    
}
