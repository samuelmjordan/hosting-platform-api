package com.mc_host.api.model.resource;

public record PterodactylAllocation(
    String subscriptionId,
    Long allocationId,
    String ip,
    Integer port,
    String alias
) {}
