package com.mc_host.api.queue;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.ResourceType;
import com.mc_host.api.model.cache.CacheNamespace;
import com.mc_host.api.model.cache.Queue;
import com.mc_host.api.service.reconciliation.ResourceReconcilerSupplier;
import com.mc_host.api.util.CacheService;

@Service
public class ReconciliationConsumer extends AbstractQueueConsumer {
    private static final Logger LOGGER = Logger.getLogger(ReconciliationConsumer.class.getName());
    private static final CacheNamespace IN_PROGRESS_FLAG = CacheNamespace.RECONCILIATION_IN_PROGRESS;
    
    private final ResourceReconcilerSupplier resourceReconcilerSupplier;
    
    public ReconciliationConsumer(
            ScheduledExecutorService scheduledExecutor,
            ExecutorService taskExecutor,
            CacheService cacheService,
            ResourceReconcilerSupplier resourceReconcilerSupplier) {
        super(scheduledExecutor, taskExecutor, cacheService);
        this.resourceReconcilerSupplier = resourceReconcilerSupplier;
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
            } else {
                cacheService.queueLeftPush(this.getQueue(), resourceType);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error reconciling resource " + resourceType, e);
            cacheService.evict(IN_PROGRESS_FLAG, resourceType);
            cacheService.queueLeftPush(this.getQueue(), resourceType);
        }
    }    
}
