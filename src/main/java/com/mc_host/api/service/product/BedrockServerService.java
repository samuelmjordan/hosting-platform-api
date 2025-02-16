package com.mc_host.api.service.product;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.entity.SubscriptionEntity;
import com.mc_host.api.model.specification.SpecificationType;

@Service
public class BedrockServerService implements ProductService {

    @Override
    public boolean isType(SpecificationType type) {
        return type.equals(SpecificationType.BEDROCK_SERVER);
    }

    @Override
    public void handle(SubscriptionEntity subscriptionEntity) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'handle'");
    }
    
}
