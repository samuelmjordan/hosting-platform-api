package com.mc_host.api.service.product;

import com.mc_host.api.model.entity.SubscriptionEntity;
import com.mc_host.api.model.specification.SpecificationType;

public interface ProductService {
    public boolean isType(SpecificationType type);
    public void handle(SubscriptionEntity subscriptionEntity); 
}
