package com.mc_host.api.model.resource.pterodactyl.response;

public record PterodactylNodeResponse(
    Attributes attributes
) {
    public record Attributes(
        Long id,
        String uuid
    ) {}
}