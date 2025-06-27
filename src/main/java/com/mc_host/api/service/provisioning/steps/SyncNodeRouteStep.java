package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.model.resource.pterodactyl.PterodactylServer;
import com.mc_host.api.repository.GameServerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SyncNodeRouteStep extends AbstractStep {

    private final GameServerRepository gameServerRepository;

    @Override
    public StepType getType() {
        return StepType.SYNC_NODE_ROUTE;
    }

    @Override
    @Transactional
    public StepTransition create(Context context) {
        //Skip for cloud resources
        if (true) {
            LOGGER.warning("%s step is illegal for cloud resources. Skipping. subId: %s".formatted(getType(), context.getSubscriptionId()));
            return transitionService.persistAndProgress(context, StepType.FINALISE);
        }

        //do

        //Early return for non-migrations
        if (!context.getMode().isMigrate()) {
            return transitionService.persistAndProgress(context, StepType.START_SERVER);
        }

        PterodactylServer pterodactylServer = gameServerRepository.selectPterodactylServer(context.getNewPterodactylServerId())
            .orElseThrow(() -> new IllegalStateException("Pterodactyl server not found: " + context.getNewPterodactylServerId()));
        String oldServerKey = gameServerRepository.selectPterodactylServer(context.getPterodactylServerId())
            .map(PterodactylServer::serverKey)
            .orElse(null);
        if (!Objects.equals(pterodactylServer.serverKey(), oldServerKey)) {
            return transitionService.persistAndProgress(context, StepType.START_SERVER);
        }
        return transitionService.persistAndProgress(context, StepType.TRANSFER_DATA);
    }

    @Override
    @Transactional
    public StepTransition destroy(Context context) {
        LOGGER.warning("%s step is illegal for destroys. Skipping. subId: %s".formatted(getType(), context.getSubscriptionId()));
        return transitionService.persistAndProgress(context, StepType.C_NAME_RECORD);
    }

}
