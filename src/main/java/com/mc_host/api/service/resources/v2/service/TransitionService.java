package com.mc_host.api.service.resources.v2.service;

import org.springframework.stereotype.Service;

import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.StepTransition;
import com.mc_host.api.service.resources.v2.context.StepType;

@Service
public class TransitionService {
    private final ServerExecutionContextRepository repository;

    public TransitionService(
        ServerExecutionContextRepository repository
    ) {
        this.repository = repository;
    }
    
    public StepTransition persistAndProgress(Context context, StepType toStep) {
        var transition = new StepTransition(
            context.transitionTo(toStep),
            toStep
        );
        repository.upsertSubscription(transition.context());
        return transition;
    }
    
    public StepTransition persistAndComplete(Context context) {
        Context completed = context.completed();
        repository.upsertSubscription(completed);
        return new StepTransition(completed, context.getStepType());
    }
}
