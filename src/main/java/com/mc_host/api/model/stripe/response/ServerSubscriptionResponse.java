package com.mc_host.api.model.stripe.response;

import com.mc_host.api.model.plan.AcceptedCurrency;
import com.mc_host.api.model.subscription.MarketingRegion;

import java.time.Instant;

public record ServerSubscriptionResponse(
    String subscriptionId,
    String serverName,
    String specificationTitle,
    String ramGb,
    String vcpu,
	String ssdGb,
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
