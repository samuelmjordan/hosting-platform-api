package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.service.panel.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferDataStep extends AbstractStep {

    private final TransferService transferService;

    @Override
    public StepType getType() {
        return StepType.TRANSFER_DATA;
    }

    @Override
    @Transactional
    public StepTransition create(Context context) {
        //Skip for non migrations
        if (!context.getMode().isMigrate()) {
            LOGGER.warning("%s step is illegal for non-migrations. Skipping. subId: %s".formatted(getType(), context.getSubscriptionId()));
            return transitionService.persistAndProgress(context, StepType.START_SERVER);
        }

        try {
            transferService.transferServerData(context.getPterodactylServerId(), context.getNewPterodactylServerId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return transitionService.persistAndProgress(context, StepType.START_SERVER);
    }

    @Override
    @Transactional
    public StepTransition destroy(Context context) {
        LOGGER.warning("%s step is illegal for destroys. Skipping. subId: %s".formatted(getType(), context.getSubscriptionId()));
        return transitionService.persistAndProgress(context, StepType.PTERODACTYL_SERVER);
    }

}
