package com.mc_host.api.model.game_server;

public record PterodactylServer(
    String serverId,
    String pterodactylServerUid,
    Long pterodactylServerId,
    Long allocationId
) {}
