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
import com.mc_host.api.util.CacheService;

@Service
public class SubscriptionSyncConsumer extends AbstractQueueConsumer {
    private static final Logger LOGGER = Logger.getLogger(SubscriptionSyncConsumer.class.getName());
    
    private final StripeConfiguration stripeConfiguration;
    private final StripeSubscriptionService stripeSubscriptionService;
    
    public SubscriptionSyncConsumer(
            ScheduledExecutorService scheduledExecutor,
            ExecutorService taskExecutor,
            CacheService cacheService,

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
                stripeSubscriptionService.handleCustomerSubscriptionSync(customerId);
            } else {
                cacheService.queuePush(this.getQueue(), customerId);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing customer " + customerId, e);
        }
    }
}