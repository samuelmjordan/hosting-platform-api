package com.mc_host.api.model.game_server;

public record PterodactylServer(
    String subscriptionId,
    String pterodactylServerUid,
    Long pterodactylServerId,
    Long allocationId
) {}
