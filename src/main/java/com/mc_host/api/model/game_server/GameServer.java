package com.mc_host.api.model.game_server;

public record GameServer(
    String serverId,
    String subscriptionId,
    String planId,
    String nodeId
) {}
