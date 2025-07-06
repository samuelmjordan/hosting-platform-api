package com.mc_host.api.model.stripe.request;

import com.mc_host.api.model.plan.AcceptedCurrency;

public record UpdateSpecificationRequest(
    String specificationId,
	AcceptedCurrency currency
) {
}
