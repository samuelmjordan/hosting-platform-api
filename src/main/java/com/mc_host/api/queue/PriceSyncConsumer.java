package com.mc_host.api.queue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.cache.Queue;
import com.mc_host.api.service.stripe.StripePriceService;
import com.mc_host.api.util.Cache;

@Service
public class PriceSyncConsumer extends AbstractQueueConsumer {
    private static final Logger LOGGER = Logger.getLogger(PriceSyncConsumer.class.getName());
    
    private final StripePriceService stripePriceService;
    
    public PriceSyncConsumer(
            ScheduledExecutorService scheduledExecutor,
            ExecutorService taskExecutor,
            Cache cacheService,

            StripePriceService stripePriceService) {
        super(scheduledExecutor, taskExecutor, cacheService);
        this.stripePriceService = stripePriceService;
    }
    
    @Override
    public Queue getQueue() {
        return Queue.PRICE_SYNC;
    }
    
    @Override
    public void processItem(String productId) {
        try {
            stripePriceService.syncPriceData(productId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing product " + productId, e);
            cacheService.queueLeftPush(this.getQueue(), productId);
        }
    }
}