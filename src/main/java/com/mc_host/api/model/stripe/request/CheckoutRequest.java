package com.mc_host.api.model.stripe.request;

import com.mc_host.api.model.subscription.MarketingRegion;

public record CheckoutRequest(
    String priceId,
    String userId,
    MarketingRegion region,
    String success,
    String cancel
) {
}
