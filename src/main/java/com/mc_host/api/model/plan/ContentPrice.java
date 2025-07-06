package com.mc_host.api.model.plan;

import java.util.Map;

public record ContentPrice(
    String priceId,
    String productId,
    Boolean active,
    Map<AcceptedCurrency, Long> minorAmounts
) {

    public Boolean isAlike(ContentPrice newPrice) {
        if (newPrice == null) {
            return false;
        }
        if (this.priceId().equals(newPrice.priceId())) {
            return true;
        }
        return false;
    }
}
