package com.mc_host.api.model.entity;

public class SubscriptionPair {
    private final ContentSubscription oldSubscription;
    private final ContentSubscription newSubscription;
    
    public SubscriptionPair(ContentSubscription oldSubscription, ContentSubscription newSubscription) {
        this.oldSubscription = oldSubscription;
        this.newSubscription = newSubscription;
    }
    
    public ContentSubscription getOldSubscription() {
        return oldSubscription;
    }
    
    public ContentSubscription getNewSubscription() {
        return newSubscription;
    }
}
