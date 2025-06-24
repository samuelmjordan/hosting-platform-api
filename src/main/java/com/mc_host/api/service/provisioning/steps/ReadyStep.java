package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.Mode;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReadyStep extends AbstractStep {

    @Override
    public StepType getType() {
        return StepType.READY;
    }

    @Override
    public StepTransition create(Context context) {
        if (context.getMode().isMigrate()) {
            return transitionService.persistAndProgress(context.withMode(Mode.MIGRATE_DESTROY), StepType.READY);
        }
        return transitionService.persistAndComplete(context);
    }

    @Override
    public StepTransition destroy(Context context) {
        return transitionService.persistAndProgress(context, StepType.C_NAME_RECORD);
    }

}
