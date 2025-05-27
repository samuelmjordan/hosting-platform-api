package com.mc_host.api.service.resources.v2.service.steps;

import java.util.logging.Logger;

import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.Mode;
import com.mc_host.api.service.resources.v2.context.StepTransition;
import com.mc_host.api.service.resources.v2.service.TransitionService;

public abstract class AbstractStep implements Step {
    protected static final Logger LOGGER = Logger.getLogger(AbstractStep.class.getName());

    protected final ServerExecutionContextRepository contextRepository;
    protected final TransitionService transitionService;

    protected AbstractStep(
        ServerExecutionContextRepository contextRepository,
        TransitionService transitionService
    ) {
        this.contextRepository = contextRepository;
        this.transitionService = transitionService;
    }

    @Override
    public StepTransition execute(Context context) {
        LOGGER.info(String.format("Starting executing step: %s for subscription: %s, mode: %s", getType(), context.getSubscriptionId(), context.getMode()));
        try {
            contextRepository.upsertSubscription(context.inProgress());
            switch (context.getMode()) {
                case Mode.CREATE:
                    return create(context);
                case Mode.DESTROY:
                    return destroy(context);
                case Mode.UPDATE:
                    return update(context);
                case Mode.MIGRATE:
                    return migrate(context);
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
