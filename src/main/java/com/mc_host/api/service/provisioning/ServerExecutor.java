package com.mc_host.api.service.provisioning;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.Status;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.service.provisioning.steps.Step;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class ServerExecutor {
    private static final Logger LOGGER = Logger.getLogger(ServerExecutor.class.getName());

    private final Map<StepType, Step> steps;

    public ServerExecutor(
        List<Step> allSteps
    ) {
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

    public void migrate(Context context) {

    }
    
}
