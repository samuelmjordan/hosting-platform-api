package com.mc_host.api.service.resources.v2.service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.Status;
import com.mc_host.api.service.resources.v2.context.StepTransition;
import com.mc_host.api.service.resources.v2.context.StepType;
import com.mc_host.api.service.resources.v2.service.steps.ARecordStep;
import com.mc_host.api.service.resources.v2.service.steps.AllocateNodeStep;
import com.mc_host.api.service.resources.v2.service.steps.CloudNodeStep;
import com.mc_host.api.service.resources.v2.service.steps.NewStep;
import com.mc_host.api.service.resources.v2.service.steps.Step;

@Service
public class ServerExecutor {
    private static final Logger LOGGER = Logger.getLogger(ServerExecutor.class.getName());

    private List<Step> steps = new ArrayList<Step>();

    public ServerExecutor(
        NewStep newStep,
        AllocateNodeStep allocateNodeStep,
        CloudNodeStep cloudNodeStep,
        ARecordStep aRecordStep
    ) {
        steps.addAll(List.of(
            newStep,
            allocateNodeStep,
            cloudNodeStep,
            aRecordStep
        ));
    }

    private Step supply(StepType stepType) {
        return steps.stream()
            .filter(step -> step.getType().equals(stepType))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Unhandled step type: " + stepType));
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
