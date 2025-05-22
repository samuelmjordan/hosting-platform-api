package com.mc_host.api.model.response;

import java.time.Instant;

import com.mc_host.api.model.AcceptedCurrency;
import com.mc_host.api.model.MarketingRegion;

public record ServerSubscriptionResponse(
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
