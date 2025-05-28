package com.mc_host.api.model.entity;

public record SubscriptionPair(
    ContentSubscription oldSubscription, 
    ContentSubscription newSubscription
) {

    public Boolean isValid() {
        return newSubscription != null || oldSubscription != null;
    }

    public Boolean isUpdated() {
        return newSubscription != null && oldSubscription != null;
    }

    public Boolean isNew() {
        return newSubscription != null && oldSubscription == null;
    }

    public Boolean isOld() {
        return newSubscription == null && oldSubscription != null;
    }
}
