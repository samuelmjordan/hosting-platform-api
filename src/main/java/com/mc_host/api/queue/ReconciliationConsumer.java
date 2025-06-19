package com.mc_host.api.queue;

import com.mc_host.api.model.cache.CacheNamespace;
import com.mc_host.api.model.cache.Queue;
import com.mc_host.api.model.resource.ResourceType;
import com.mc_host.api.service.reconciliation.ResourceReconcilerSupplier;
import com.mc_host.api.util.Cache;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ReconciliationConsumer extends AbstractQueueConsumer {
    private static final Logger LOGGER = Logger.getLogger(ReconciliationConsumer.class.getName());
    private static final CacheNamespace IN_PROGRESS_FLAG = CacheNamespace.RECONCILIATION_IN_PROGRESS;
    protected static final long MAX_DELAY_MS = 100000;
    
    private final ResourceReconcilerSupplier resourceReconcilerSupplier;
    
    public ReconciliationConsumer(
            ScheduledExecutorService scheduledExecutor,
            ExecutorService taskExecutor,
            Cache cacheService,
            ResourceReconcilerSupplier resourceReconcilerSupplier) {
        super(scheduledExecutor, taskExecutor, cacheService);
        this.resourceReconcilerSupplier = resourceReconcilerSupplier;
    }

    @Override
    protected long getMaxDelayMs() {
        return MAX_DELAY_MS;
    }
    
    @Override
    public Queue getQueue() {
        return Queue.RESOURCE_RECONCILE;
    }
    
    @Override
    public void processItem(String resourceType) {
        try {
            if (cacheService.flagIfAbsent(
                    IN_PROGRESS_FLAG, 
                    resourceType,    
                    Duration.ofMinutes(30))) {
                resourceReconcilerSupplier.supply(ResourceType.valueOf(resourceType)).reconcile();
                cacheService.evict(IN_PROGRESS_FLAG, resourceType);
                resetBackoff();
            } else {
                cacheService.queueLeftPush(this.getQueue(), resourceType);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error reconciling resource " + resourceType, e);
            cacheService.evict(IN_PROGRESS_FLAG, resourceType);
            applyBackoff();
            requeueItem(resourceType);
            throw new RuntimeException("Failed to reconcile resource: " + resourceType, e);
        }
    }    
}
