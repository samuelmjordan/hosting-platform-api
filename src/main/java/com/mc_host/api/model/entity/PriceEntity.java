package com.mc_host.api.model.entity;

import com.mc_host.api.model.Currency;

public record PriceEntity(
    String priceId,
    String productId,
    Boolean active,
    Currency currency,
    Long minorAmount
) {
}
