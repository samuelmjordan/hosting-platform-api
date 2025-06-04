package com.mc_host.api.queue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.cache.Queue;
import com.mc_host.api.model.stripe.StripeEventType;
import com.mc_host.api.service.stripe.events.StripeEventService;
import com.mc_host.api.util.Cache;

@Service
public class StripeEventConsumer extends AbstractQueueConsumer {
    private static final Logger LOGGER = Logger.getLogger(StripeEventConsumer.class.getName());
    protected static final long MAX_DELAY_MS = 5000;

    private final Map<StripeEventType, StripeEventService> stripeEventServices;

    public StripeEventConsumer (
        ScheduledExecutorService scheduledExecutor,
        ExecutorService taskExecutor,
        Cache cacheService,
        List<StripeEventService> allStripeEventServices
    ) {
        super(scheduledExecutor, taskExecutor, cacheService);
        this.stripeEventServices = allStripeEventServices.stream()
            .collect(Collectors.toMap(
                StripeEventService::getType, 
                Function.identity()
            ));
    }

    @Override
    protected long getMaxDelayMs() {
        return MAX_DELAY_MS;
    }
    
    @Override
    public Queue getQueue() {
        return Queue.STRIPE_EVENT;
    }
    
    @Override
    public void processItem(String event) {
        try {
            String[] parts = event.split(":", 2);
            StripeEventType type = StripeEventType.valueOf(parts[0]);
            String id = parts[1];

            stripeEventServices.get(type).process(id);
            resetBackoff();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing stripe event: " + event, e);
            applyBackoff();
            requeueItem(event);
            throw new RuntimeException("Failed to process stripe event: " + event, e);
        }
    }
}