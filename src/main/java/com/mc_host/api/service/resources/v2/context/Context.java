package com.mc_host.api.service.resources.v2.context;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;

@Getter
@With
@AllArgsConstructor
public class Context {
    private final String subscriptionId;
    private final StepType stepType;
    private final Mode mode;
    private final Status status;

    public Context inProgress() {
        return this.withStatus(Status.IN_PROGRESS);
    }

    public Context completed() {
        return this.withStatus(Status.COMPLETED);
    }

    public Context failed() {
        return this.withStatus(Status.FAILED);
    }
    
    public Context transitionTo(StepType nextStep) {
        return this.withStepType(nextStep).inProgress();
    }
    
    // convenience factory method
    public static Context create(String subscriptionId, Mode mode) {
        return new Context(
            subscriptionId,
            StepType.NEW,
            mode,
            Status.IN_PROGRESS
        );
    }
}