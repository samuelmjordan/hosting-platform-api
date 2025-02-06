package com.mc_host.api.model.entity;

import java.time.Instant;

public record SubscriptionEntity(
    String subscriptionId,
    String customerId,
    String status,
    String priceId,
    Instant currentPeriodEnd,
    Instant currentPeriodStart,
    Boolean cancelAtPeriodEnd) {
}
