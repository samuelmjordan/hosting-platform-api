package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.provisioning.TransitionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
