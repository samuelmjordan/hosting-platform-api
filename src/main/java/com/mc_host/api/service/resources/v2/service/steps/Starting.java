package com.mc_host.api.service.resources.v2.service.steps;

import org.springframework.stereotype.Service;

import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.StepTransition;
import com.mc_host.api.service.resources.v2.context.StepType;
import com.mc_host.api.service.resources.v2.service.TransitionService;

@Service
public class Starting extends AbstractStep {

    protected Starting(
        ServerExecutionContextRepository contextRepository,
        TransitionService transitionService
    ) {
        super(contextRepository, transitionService);
    }

    @Override
    public StepType getType() {
        return StepType.STARTING;
    }

    @Override
    public StepTransition create(Context context) {
        return transitionService.persistAndProgress(context, StepType.ALLOCATE_NODE);
    }

    @Override
    public StepTransition destroy(Context context) {
        contextRepository.promoteNewResourcesToCurrent(context.getSubscriptionId());
        return transitionService.persistAndComplete(context);
    }

}
