package com.mc_host.api.service.resources.v2.context;

public record StepTransition(
    Context context,
    StepType toStep
) {}
