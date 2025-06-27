package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinaliseStep extends AbstractStep {

    @Override
    public StepType getType() {
        return StepType.FINALISE;
    }

    @Override
    @Transactional
    public StepTransition create(Context context) {
        //Skip for migrations
        if (context.getMode().isMigrate()) {
            LOGGER.warning("%s step is illegal for migrations. Skipping. subId: %s".formatted(getType(), context.getSubscriptionId()));
            return transitionService.persistAndProgress(context, StepType.READY);
        }

        Context transitionedContext = context.promoteAllNewResources();
        return transitionService.persistAndProgress(transitionedContext, StepType.READY);
    }

    @Override
    @Transactional
    public StepTransition destroy(Context context) {
        LOGGER.warning("%s step is illegal for destroys. Skipping. subId: %s".formatted(getType(), context.getSubscriptionId()));
        return transitionService.persistAndProgress(context, StepType.C_NAME_RECORD);
    }
}
