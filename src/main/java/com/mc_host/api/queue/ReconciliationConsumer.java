package com.mc_host.api.queue;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.cache.CacheNamespace;
import com.mc_host.api.model.cache.Queue;
import com.mc_host.api.service.reconciliation.HetznerNodeReconciler;
import com.mc_host.api.util.CacheService;

@Service
public class ReconciliationConsumer extends AbstractQueueConsumer {
    private static final Logger LOGGER = Logger.getLogger(ReconciliationConsumer.class.getName());
    private static final CacheNamespace IN_PROGRESS_FLAG = CacheNamespace.RECONCILIATION_IN_PROGRESS;

    protected static final long INITIAL_DELAY_MS = 60_000;
    protected static final long MAX_DELAY_MS = 600_000;
    
    private final HetznerNodeReconciler hetznerNodeReconciler;
    
    public ReconciliationConsumer(
            ScheduledExecutorService scheduledExecutor,
            ExecutorService taskExecutor,
            CacheService cacheService,

            HetznerNodeReconciler hetznerNodeReconciler) {
        super(scheduledExecutor, taskExecutor, cacheService);
        this.hetznerNodeReconciler = hetznerNodeReconciler;
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
                hetznerNodeReconciler.reconcile();
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
