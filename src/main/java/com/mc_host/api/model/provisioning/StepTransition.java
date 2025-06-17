package com.mc_host.api.model.provisioning;

public record StepTransition(
    Context context,
    StepType toStep
) {}
