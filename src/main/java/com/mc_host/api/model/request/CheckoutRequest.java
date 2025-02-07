package com.mc_host.api.model.request;

public record CheckoutRequest(
    String priceId,
    String userId,
    String success,
    String cancel
) {
}
