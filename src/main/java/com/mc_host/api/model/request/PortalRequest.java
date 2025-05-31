package com.mc_host.api.model.request;

public record PortalRequest(
    String returnUrl,
    String userId
) {}
