package com.mc_host.api.model.pterodactyl.request;

public record SessionRequest(
    String userId,
    String path
) {
}
