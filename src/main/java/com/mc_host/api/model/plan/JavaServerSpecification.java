package com.mc_host.api.model.plan;

public record JavaServerSpecification(
    String specification_id,
    String title,
    String description,
    String ram_gb,
    String vcpu,
    String ssd_gb
) implements Specification{
    @Override
    public SpecificationType type() { return SpecificationType.GAME_SERVER; }
}