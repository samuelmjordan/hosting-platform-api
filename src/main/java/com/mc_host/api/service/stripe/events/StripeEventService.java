package com.mc_host.api.service.stripe.events;

import com.mc_host.api.model.cache.StripeEventType;

public interface StripeEventService {
    public StripeEventType getType();

    public void process(String id);
}
