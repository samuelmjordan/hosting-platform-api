package com.mc_host.api.service.resources.v2.service.steps;

import org.springframework.stereotype.Service;

import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.StepTransition;
import com.mc_host.api.service.resources.v2.context.StepType;
import com.mc_host.api.service.resources.v2.service.TransitionService;

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
        return transitionService.persistAndComplete(context);
    }

    @Override
    public StepTransition destroy(Context context) {
        return transitionService.persistAndProgress(context, StepType.C_NAME_RECORD);
    }

}
