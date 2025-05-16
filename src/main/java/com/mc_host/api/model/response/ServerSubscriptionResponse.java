package com.mc_host.api.model.response;

import java.time.Instant;

import com.mc_host.api.model.AcceptedCurrency;
import com.mc_host.api.model.MarketingRegion;

public record ServerSubscriptionResponse(
    String name,
    String subscriptionStatus,
    Instant currentPeriodEnd,
    Instant CurrentPeriodStart,
    Boolean cancelAtPeriodEnd,
    AcceptedCurrency currency,
    Long minorAmount,
    MarketingRegion regionCode,
    String cnameRecordName
) {
}
