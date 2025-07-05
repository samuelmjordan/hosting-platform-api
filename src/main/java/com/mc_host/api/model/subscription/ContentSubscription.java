package com.mc_host.api.model.subscription;

import com.mc_host.api.model.stripe.SubscriptionStatus;

import java.time.Instant;

public record ContentSubscription(
    String subscriptionId,
    String customerId,
    SubscriptionStatus status,
    String priceId,
    Instant currentPeriodEnd,
    Instant currentPeriodStart,
    Boolean cancelAtPeriodEnd,
    String subdomain
) {

}
