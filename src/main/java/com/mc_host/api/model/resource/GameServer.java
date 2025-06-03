package com.mc_host.api.model.resource;

public record GameServer(
    String serverId,
    String subscriptionId,
    String planId,
    String nodeId
) {}
