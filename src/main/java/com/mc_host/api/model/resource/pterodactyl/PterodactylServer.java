package com.mc_host.api.model.resource.pterodactyl;

public record PterodactylServer(
    String subscriptionId,
    String pterodactylServerUid,
    Long pterodactylServerId,
    Long allocationId
) {}
