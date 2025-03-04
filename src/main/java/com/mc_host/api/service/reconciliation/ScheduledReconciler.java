package com.mc_host.api.service.reconciliation;

import java.util.logging.Logger;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import com.mc_host.api.model.ResourceType;
import com.mc_host.api.model.cache.Queue;
import com.mc_host.api.util.Cache;
import com.mc_host.api.util.Task;

@Service
@Controller
public class ScheduledReconciler {
    private static final Logger LOGGER = Logger.getLogger(ScheduledReconciler.class.getName());
    private static final Queue QUEUE = Queue.RESOURCE_RECONCILE;

    private final Cache cacheService;

    public ScheduledReconciler(
        Cache cacheService
    ) {
        this.cacheService = cacheService;
    }

    @Scheduled(cron = "0 0 11,18 * * ?", zone = "UTC")
    @GetMapping("/hetzner")
    public void reconcileAllResources() {
        for (ResourceType resourceType : ResourceType.values()) {
            Task.alwaysAttempt(
                String.format("Resource reconciliation for type %S", resourceType), 
                ()  -> {
                    cacheService.queueLeftPush(QUEUE, resourceType.name());
                    LOGGER.info(String.format("Pushed resource type %s for reconciliation", resourceType));
                }
            );
        }
    }
    
}
