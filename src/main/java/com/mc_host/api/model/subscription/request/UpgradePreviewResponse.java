package com.mc_host.api.model.subscription.request;

import com.mc_host.api.model.plan.AcceptedCurrency;

public record UpgradePreviewResponse(
	Long immediateCharge,
	Long newMonthlyAmount,
	Long oldMonthlyAmount,
	AcceptedCurrency currency
) {
}
