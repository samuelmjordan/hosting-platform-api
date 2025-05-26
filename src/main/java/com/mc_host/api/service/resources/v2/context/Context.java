package com.mc_host.api.service.resources.v2.context;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;

@Getter
@AllArgsConstructor
public class Context {

    String subscriptionId;
    @With StepType stepType;
    Mode mode;
    Status status;

    public Context inProgress() {
        this.status = Status.IN_PROGRESS;
        return this;
    }

    public Context completed() {
        this.status = Status.COMPLETED;
        return this;
    }

    public Context failed() {
        this.status = Status.FAILED;
        return this;
    }
}
