package com.mc_host.api.model.stripe;

import java.time.Instant;

import com.mc_host.api.model.plan.AcceptedCurrency;

public record CustomerInvoice(
    String invoiceId,
    String customerId,
    String subscriptionId,
    String invoiceNumber,
    Boolean paid,
    String paymentMethod,
    String collectionMethod,
    AcceptedCurrency currency,
    Long minorAmount,
    Instant createdAt,
    String link) {
}
