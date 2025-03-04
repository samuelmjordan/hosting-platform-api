package com.mc_host.api.model.request;

import com.mc_host.api.model.MarketingRegion;

public record CheckoutRequest(
    String priceId,
    String userId,
    MarketingRegion region,
    String success,
    String cancel
) {
}
