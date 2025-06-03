package com.mc_host.api.model.plan;

public record Plan(
    Specification specification,
    ContentPrice price
) {
}
