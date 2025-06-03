package com.mc_host.api.model.plan;

public record ContentPrice(
    String priceId,
    String productId,
    Boolean active,
    AcceptedCurrency currency,
    Long minorAmount
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
