package com.mc_host.api.model.pterodactyl.response;

public record PterodactylNodeResponse(
    Attributes attributes
) {
    public record Attributes(
        Integer id,
        String uuid
    ) {}
}