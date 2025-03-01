package com.mc_host.api.model.game_server;

public record PterodactylServer(
    String serverId,
    Long pterodactylServerId,
    Long allocationId,
    Integer port
) {}
