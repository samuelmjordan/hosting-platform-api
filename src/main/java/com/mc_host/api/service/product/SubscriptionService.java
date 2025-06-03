package com.mc_host.api.service.product;

import com.mc_host.api.model.plan.SpecificationType;
import com.mc_host.api.model.subscription.ContentSubscription;

public interface SubscriptionService {
    public boolean isType(SpecificationType type); 

    public void create(ContentSubscription newSubscription); 
    public void delete(ContentSubscription oldSubscription);
    public void update(ContentSubscription newSubscription, ContentSubscription oldSubscription); 
}
