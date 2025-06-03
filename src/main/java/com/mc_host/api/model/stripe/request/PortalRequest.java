package com.mc_host.api.model.stripe.request;

public record PortalRequest(
    String returnUrl,
    String userId
) {}
