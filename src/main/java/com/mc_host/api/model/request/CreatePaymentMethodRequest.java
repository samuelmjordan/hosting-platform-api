package com.mc_host.api.model.request;

import com.mc_host.api.model.AcceptedCurrency;

public record CreatePaymentMethodRequest(
    String success,
    String cancel,
    AcceptedCurrency currency
) {
    
}
