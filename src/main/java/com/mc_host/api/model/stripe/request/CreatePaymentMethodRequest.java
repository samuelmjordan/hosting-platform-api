package com.mc_host.api.model.stripe.request;

public record CreatePaymentMethodRequest(
    String success,
    String cancel
) {
    
}
