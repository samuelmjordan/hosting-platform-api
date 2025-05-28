package com.mc_host.api.service.resources.v2.service.steps;

import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.StepTransition;
import com.mc_host.api.service.resources.v2.context.StepType;

public interface Step {
    public StepType getType();

    public StepTransition execute(Context context);
    public StepTransition create(Context context);
    public StepTransition destroy(Context context);

}
