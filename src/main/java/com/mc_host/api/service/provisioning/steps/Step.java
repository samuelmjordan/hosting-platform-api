package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;

public interface Step {
    public StepType getType();

    public StepTransition execute(Context context);
    public StepTransition create(Context context);
    public StepTransition destroy(Context context);

}
