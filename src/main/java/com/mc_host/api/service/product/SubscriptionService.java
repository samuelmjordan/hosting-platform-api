package com.mc_host.api.service.product;

import com.mc_host.api.model.entity.ContentSubscription;
import com.mc_host.api.model.specification.SpecificationType;

public interface SubscriptionService {
    public boolean isType(SpecificationType type); 

    public void create(ContentSubscription newSubscription); 
    public void delete(ContentSubscription oldSubscription);
    public void update(ContentSubscription newSubscription, ContentSubscription oldSubscription); 
}
