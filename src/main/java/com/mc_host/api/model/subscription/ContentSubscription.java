package com.mc_host.api.model.subscription;

import com.mc_host.api.model.plan.AcceptedCurrency;
import com.mc_host.api.model.stripe.SubscriptionStatus;

import java.time.Instant;

public record ContentSubscription(
    String subscriptionId,
    String customerId,
    SubscriptionStatus status,
    String priceId,
	AcceptedCurrency currency,
    Instant currentPeriodEnd,
    Instant currentPeriodStart,
    Boolean cancelAtPeriodEnd,
    String subdomain
) {

}
