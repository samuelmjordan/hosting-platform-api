package com.mc_host.api.queue;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.configuration.StripeConfiguration;
import com.mc_host.api.model.cache.CacheNamespace;
import com.mc_host.api.model.cache.Queue;
import com.mc_host.api.service.stripe.StripeSubscriptionService;
import com.mc_host.api.util.Cache;

@Service
public class SubscriptionSyncConsumer extends AbstractQueueConsumer {
    private static final Logger LOGGER = Logger.getLogger(SubscriptionSyncConsumer.class.getName());
    
    private final StripeConfiguration stripeConfiguration;
    private final StripeSubscriptionService stripeSubscriptionService;
    
    public SubscriptionSyncConsumer(
            ScheduledExecutorService scheduledExecutor,
            ExecutorService taskExecutor,
            Cache cacheService,

            StripeConfiguration stripeConfiguration,
            StripeSubscriptionService stripeSubscriptionService) {
        super(scheduledExecutor, taskExecutor, cacheService);
        this.stripeConfiguration = stripeConfiguration;
        this.stripeSubscriptionService = stripeSubscriptionService;
    }
    
    @Override
    public Queue getQueue() {
        return Queue.SUBSCRIPTION_SYNC;
    }
    
    @Override
    public void processItem(String customerId) {
        try {
            if (cacheService.flagIfAbsent(
                    CacheNamespace.SUBSCRIPTION_SYNC_IN_PROGRESS, 
                    customerId,    
                    Duration.ofMinutes(stripeConfiguration.getSubscriptionSyncTimeoutMinutes()))) {
                stripeSubscriptionService.handleCustomerSubscriptionSyncV2(customerId);
                cacheService.evict(CacheNamespace.SUBSCRIPTION_SYNC_IN_PROGRESS, customerId);
                resetBackoff();
            } else {
                cacheService.queueLeftPush(this.getQueue(), customerId);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing customer " + customerId, e);
            cacheService.evict(CacheNamespace.SUBSCRIPTION_SYNC_IN_PROGRESS, customerId);
            applyBackoff();
            requeueItem(customerId);
            throw new RuntimeException("Failed to process subscription sync for customer: " + customerId, e);
        }
    }
}