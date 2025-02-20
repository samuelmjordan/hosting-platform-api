package com.mc_host.api.model.entity.node.pterodactyl_response;

public record PterodactylNodeResponse(
    String name,
    String description,
    Integer locationId,
    Boolean public_,
    String fqdn,
    String scheme,
    Integer memory,
    Integer memoryOverallocate,
    Integer disk,
    Integer diskOverallocate,
    Integer uploadSize,
    Integer daemonSftp,
    Integer daemonListen
) {

}