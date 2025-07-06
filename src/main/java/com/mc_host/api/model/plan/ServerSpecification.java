package com.mc_host.api.model.plan;

public record ServerSpecification(
    String specification_id,
    String title,
    String description,
    Integer ram_gb,
    Integer vcpu,
    Integer ssd_gb
) implements Specification{
    @Override
    public SpecificationType type() { return SpecificationType.GAME_SERVER; }
}