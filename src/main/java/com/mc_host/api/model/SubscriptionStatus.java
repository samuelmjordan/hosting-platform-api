package com.mc_host.api.model;

public enum SubscriptionStatus {
    ACTIVE("active"),
    TRIALING("trialing"),
    PAST_DUE("past_due"),
    UNPAID("unpaid"),
    INCOMPLETE("incomplete"),
    INCOMPLETE_EXPIRED("incomplete_expired"),
    CANCELED("canceled"),
    ENDED("ended"),
    PAUSED("paused");

    private final String stripeValue;

    SubscriptionStatus(String stripeValue) {
        this.stripeValue = stripeValue;
    }

    public String toString() {
        return stripeValue;
    }

    public static SubscriptionStatus fromString(String stripeValue) {
        for (SubscriptionStatus status : values()) {
            if (status.stripeValue.equals(stripeValue)) {
                return status;
            }
        }
        throw new IllegalArgumentException("unknown subscription status: " + stripeValue);
    }

    public boolean isActive() {
        return this == ACTIVE || this == TRIALING;
    }

    public boolean isPending() {
        return this == INCOMPLETE || this == INCOMPLETE_EXPIRED || this == PAUSED;
    }

    public boolean isDelinquent() {
        return this == PAST_DUE || this == UNPAID;
    }

    public boolean isTerminated() {
        return this == CANCELED || this == ENDED;
    }
}
