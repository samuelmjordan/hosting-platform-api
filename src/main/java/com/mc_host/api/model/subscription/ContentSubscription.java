package com.mc_host.api.model.subscription;

import java.time.Instant;

import com.mc_host.api.model.stripe.SubscriptionStatus;

public record ContentSubscription(
    String subscriptionId,
    String customerId,
    SubscriptionStatus status,
    String priceId,
    Instant currentPeriodEnd,
    Instant currentPeriodStart,
    Boolean cancelAtPeriodEnd,
    MarketingRegion initialRegion
) {
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
