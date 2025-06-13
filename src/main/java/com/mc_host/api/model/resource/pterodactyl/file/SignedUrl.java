package com.mc_host.api.model.resource.pterodactyl.file;

public record SignedUrl(
    String object,
    SignedUrlAttributes attributes
) {
    public record SignedUrlAttributes(
        String url
    ) {}
}