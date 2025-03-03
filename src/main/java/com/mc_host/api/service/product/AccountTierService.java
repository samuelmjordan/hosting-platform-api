package com.mc_host.api.service.product;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.entity.ContentSubscription;
import com.mc_host.api.model.specification.SpecificationType;

@Service
public class AccountTierService implements SubscriptionService {

    @Override
    public boolean isType(SpecificationType type) {
        return type.equals(SpecificationType.ACCOUNT_TIER);
    }

    @Override
    public void create(ContentSubscription newSubscription) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'create'");
    }

    @Override
    public void delete(ContentSubscription oldSubscription) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public void update(ContentSubscription newSubscription, ContentSubscription oldSubscription) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'update'");
    }
    
}
