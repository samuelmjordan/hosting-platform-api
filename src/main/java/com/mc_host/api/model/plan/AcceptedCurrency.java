package com.mc_host.api.model.plan;

public enum AcceptedCurrency {
    XXX,
    USD,
    EUR,
    GBP;

    public static AcceptedCurrency fromCode(String code) {
        return AcceptedCurrency.valueOf(code.toUpperCase());
    }
}
