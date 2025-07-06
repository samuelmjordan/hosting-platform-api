package com.mc_host.api.service.provisioning;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class TransitionService {
    private static final Logger LOGGER = Logger.getLogger(TransitionService.class.getName());

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
        LOGGER.info(String.format("Finished executing at step: %s for subscription: %s, mode: %s", context.getStepType(), context.getSubscriptionId(), context.getMode()));
        return new StepTransition(completed, context.getStepType());
    }
}
