package com.mc_host.api.model.cache;

import lombok.Getter;

@Getter
public enum StripeEventType {
    INVOICE,
    SUBSCRIPTION,
    PAYMENT_METHOD,
    PRICE
}
