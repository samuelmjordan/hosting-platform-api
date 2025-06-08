package com.mc_host.api.model.resource.pterodactyl.request;

public record NewDirectoryRequest(
    String root,
    String name
) {
    
}
