package com.mc_host.api.service.resources.v2.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.Status;
import com.mc_host.api.service.resources.v2.context.StepTransition;
import com.mc_host.api.service.resources.v2.context.StepType;
import com.mc_host.api.service.resources.v2.service.steps.Step;

@Service
public class ServerExecutor {
    private static final Logger LOGGER = Logger.getLogger(ServerExecutor.class.getName());

    private final Map<StepType, Step> steps;

    public ServerExecutor(List<Step> allSteps) {
        this.steps = allSteps.stream()
            .collect(Collectors.toMap(
                Step::getType, 
                Function.identity()
            ));
    }

    private Step supply(StepType stepType) {
        return Optional.ofNullable(steps.get(stepType))
            .orElseThrow(() -> new IllegalStateException("Unhandled step: " + stepType));
    }

    public void execute(Context context) {
        LOGGER.info(String.format("Starting execution for subscription: %s", context.getSubscriptionId()));

        StepTransition transition = new StepTransition(context, context.getStepType());
        while (!transition.context().getStatus().equals(Status.COMPLETED)) {
            transition = supply(transition.toStep()).execute(transition.context());
        }

        LOGGER.info(String.format("Execution completed for subscription: %s", context.getSubscriptionId()));
    }
    
}
