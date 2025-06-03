package com.mc_host.api.model.resource.pterodactyl.request;

public record SessionRequest(
    String userId,
    String path
) {
}
