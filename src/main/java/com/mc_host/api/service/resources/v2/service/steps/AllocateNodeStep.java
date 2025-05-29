package com.mc_host.api.service.resources.v2.service.steps;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.StepTransition;
import com.mc_host.api.service.resources.v2.context.StepType;
import com.mc_host.api.service.resources.v2.service.TransitionService;

@Service
public class AllocateNodeStep extends AbstractStep {

    protected AllocateNodeStep(
        ServerExecutionContextRepository contextRepository,
        TransitionService transitionService
    ) {
        super(contextRepository, transitionService);
    }

    @Override
    public StepType getType() {
        return StepType.ALLOCATE_NODE;
    }

    @Override
    @Transactional
    @SuppressWarnings("unused")
    public StepTransition create(Context context) {
        // TODO: use dedicated node if available
        if (false) {
            return transitionService.persistAndProgress(context, StepType.DEDICATED_NODE);
        }
        return transitionService.persistAndProgress(context, StepType.CLOUD_NODE);
    }

    @Override
    @Transactional
    public StepTransition destroy(Context context) {
        return transitionService.persistAndProgress(context, StepType.NEW);
    }

}
