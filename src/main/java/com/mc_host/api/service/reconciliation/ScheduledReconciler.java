package com.mc_host.api.service.reconciliation;

import com.mc_host.api.model.resource.ResourceType;
import com.mc_host.api.model.queue.JobType;
import com.mc_host.api.queue.JobScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
@Controller
public class ScheduledReconciler {
    private static final Logger LOGGER = Logger.getLogger(ScheduledReconciler.class.getName());

    private final JobScheduler jobScheduler;

    public ScheduledReconciler(
        JobScheduler jobScheduler
    ) {
        this.jobScheduler = jobScheduler;
    }

    @Scheduled(fixedDelay = 1000*60*60)
    public void reconcileAllResources() {
        for (ResourceType resourceType : ResourceType.values()) {
            jobScheduler.schedule(JobType.RECONCILE_RESOURCE_TYPE, resourceType.name());
        }
    }
    
}
