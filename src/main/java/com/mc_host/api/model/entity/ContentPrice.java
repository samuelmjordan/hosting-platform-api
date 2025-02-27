package com.mc_host.api.model.entity;

import com.mc_host.api.model.AcceptedCurrency;

public record ContentPrice(
    String priceId,
    String productId,
    Boolean active,
    AcceptedCurrency currency,
    Long minorAmount
) {
}
