package com.mc_host.api.model.node;

public record PterodactylAllocation(
    Long allocationId,
    String subscriptionId,
    String ip,
    Integer port,
    String alias
) {}
