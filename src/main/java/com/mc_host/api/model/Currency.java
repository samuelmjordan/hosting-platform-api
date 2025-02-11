package com.mc_host.api.model;

public enum Currency {
    XXX,
    USD,
    EUR,
    GBP;

    public String toCode() {
        return this.name().toLowerCase();
    }

    public static Currency fromCode(String code) {
        return Currency.valueOf(code.toUpperCase());
    }
}
