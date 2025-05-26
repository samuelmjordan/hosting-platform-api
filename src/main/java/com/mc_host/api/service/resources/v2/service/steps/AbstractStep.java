package com.mc_host.api.service.resources.v2.service.steps;

import java.util.logging.Logger;

import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.Mode;
import com.mc_host.api.service.resources.v2.context.StepTransition;
import com.mc_host.api.service.resources.v2.context.StepType;

public abstract class AbstractStep implements Step {
    protected static final Logger LOGGER = Logger.getLogger(AbstractStep.class.getName());

    protected final ServerExecutionContextRepository contextRepository;

    protected AbstractStep(
        ServerExecutionContextRepository contextRepository
    ) {
        this.contextRepository = contextRepository;
    }

    protected StepTransition complete(Context context) {
        contextRepository.upsertSubscription(context.completed());
        return new StepTransition(context, context.getStepType());
    }

    protected StepTransition inProgress(Context context, StepType nextStepType) {
        Context nextContext = context.inProgress().withStepType(nextStepType);
        contextRepository.upsertSubscription(nextContext);
        return new StepTransition(nextContext, nextStepType);
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
