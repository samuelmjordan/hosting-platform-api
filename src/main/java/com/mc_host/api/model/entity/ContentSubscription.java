package com.mc_host.api.model.entity;

import java.time.Instant;
import java.util.Map;

public record ContentSubscription(
    String subscriptionId,
    String customerId,
    String status,
    String priceId,
    Instant currentPeriodEnd,
    Instant currentPeriodStart,
    Boolean cancelAtPeriodEnd,
    Map<String, String> metadata) {

    public Boolean isAlike(ContentSubscription newSubscription) {
        if (newSubscription == null) {
            return false;
        }
        if (this.subscriptionId().equals(newSubscription.subscriptionId())) {
            return true;
        }
        return false;
    }
}
