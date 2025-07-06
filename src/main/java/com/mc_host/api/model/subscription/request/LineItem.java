package com.mc_host.api.model.subscription.request;

public record LineItem(
	String reason,
	Long amount,
	String currency
) {
}
