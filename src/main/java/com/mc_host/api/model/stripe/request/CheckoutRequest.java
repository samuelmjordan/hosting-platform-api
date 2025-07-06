package com.mc_host.api.model.stripe.request;

import com.mc_host.api.model.plan.AcceptedCurrency;

public record CheckoutRequest(
    String priceId,
	AcceptedCurrency currency,
    String success,
    String cancel
) {
}
