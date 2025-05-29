package com.mc_host.api.service.resources.v2.service.steps;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.StepTransition;
import com.mc_host.api.service.resources.v2.context.StepType;
import com.mc_host.api.service.resources.v2.service.TransitionService;

@Service
public class FinaliseStep extends AbstractStep {

    protected FinaliseStep(
        ServerExecutionContextRepository contextRepository,
        TransitionService transitionService
    ) {
        super(contextRepository, transitionService);
    }

    @Override
    public StepType getType() {
        return StepType.FINALISE;
    }

    @Override
    @Transactional
    public StepTransition create(Context context) {
        Context transitionedContext = context;
        if (!context.getMode().isMigrate()) {
            transitionedContext = context.promoteAllNewResources();
        }

        return transitionService.persistAndProgress(transitionedContext, StepType.READY);
    }

    @Override
    @Transactional
    public StepTransition destroy(Context context) {
        return transitionService.persistAndProgress(context, StepType.C_NAME_RECORD);
    }
}
