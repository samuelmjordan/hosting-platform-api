package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.Mode;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.provisioning.TransitionService;
import org.springframework.stereotype.Service;

@Service
public class ReadyStep extends AbstractStep {

    protected ReadyStep(
        ServerExecutionContextRepository contextRepository,
        TransitionService transitionService
    ) {
        super(contextRepository, transitionService);
    }

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
