package com.mc_host.api.model.stripe.request;

public record CheckoutRequest(
    String priceId,
    String success,
    String cancel
) {
}
