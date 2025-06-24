package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.provisioning.TransitionService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.logging.Logger;

public abstract class AbstractStep implements Step {
    protected static final Logger LOGGER = Logger.getLogger(AbstractStep.class.getName());

    @Autowired protected ServerExecutionContextRepository contextRepository;
    @Autowired protected TransitionService transitionService;

    @Override
    public StepTransition execute(Context context) {
        LOGGER.info(String.format("Starting executing step: %s for subscription: %s, mode: %s", getType(), context.getSubscriptionId(), context.getMode()));
        try {
            contextRepository.upsertSubscription(context.inProgress());
            if (context.getMode().isCreate()) {
                return create(context);
            } else if (context.getMode().isDestroy()) {
                return destroy(context);
            }         
        } catch (Exception e) {
            contextRepository.upsertSubscription(context.failed());
            throw new RuntimeException(
                String.format("Failed executing step: %s for subscription: %s, mode: %s", getType(), context.getSubscriptionId(), context.getMode()), e
            );
        }

        throw new IllegalStateException(
            String.format("Unsupported mode: %s for step: %s", context.getMode(), getType())
        );
    }

}
