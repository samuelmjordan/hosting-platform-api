package com.mc_host.api.model.stripe;

import lombok.Getter;

@Getter
public enum StripeEventType {
    INVOICE,
    SUBSCRIPTION,
    PAYMENT_METHOD,
    PRICE
}
