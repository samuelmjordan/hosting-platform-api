package com.mc_host.api.model.subscription.request;

import com.mc_host.api.model.plan.AcceptedCurrency;

public record UpgradeConfirmationResponse(
	Long chargedAmount,
	Long newMonthlyAmount,
	AcceptedCurrency currency,
	String invoiceId
) {
}
