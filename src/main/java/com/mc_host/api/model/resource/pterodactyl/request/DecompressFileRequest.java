package com.mc_host.api.model.resource.pterodactyl.request;

public record DecompressFileRequest(
    String root,
    String file
) {
    
}
