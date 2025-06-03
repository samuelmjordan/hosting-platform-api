package com.mc_host.api.model.stripe.response;

import java.time.Instant;

import com.mc_host.api.model.plan.AcceptedCurrency;
import com.mc_host.api.model.subscription.MarketingRegion;

public record ServerSubscriptionResponse(
    String subscriptionId,
    String serverName,
    String specificationTitle,
    String ramGb,
    String vcpu,
    MarketingRegion regionCode,
    String cnameRecordName,
    String subscriptionStatus,
    Instant currentPeriodEnd,
    Instant currentPeriodStart,
    Boolean cancelAtPeriodEnd,
    AcceptedCurrency currency,
    Long minorAmount
) {
}
