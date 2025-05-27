package com.mc_host.api.model.node;

public record PterodactylAllocation(
    Long allocation_id,
    Long pterodactyl_node_id,
    String ip,
    Integer port,
    String alias
) {}
