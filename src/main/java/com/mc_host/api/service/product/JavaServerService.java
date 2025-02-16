package com.mc_host.api.service.product;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.entity.JavaServerEntity;
import com.mc_host.api.model.entity.SubscriptionEntity;
import com.mc_host.api.model.specification.SpecificationType;
import com.mc_host.api.persistence.JavaServerPersistenceService;
import com.mc_host.api.persistence.PlanPersistenceService;

@Service
public class JavaServerService implements ProductService  {
    private static final Logger LOGGER = Logger.getLogger(JavaServerService.class.getName());

    private final JavaServerPersistenceService javaServerPersistenceService;
    private final PlanPersistenceService planPersistenceService;

    JavaServerService(
        JavaServerPersistenceService javaServerPersistenceService,
        PlanPersistenceService planPersistenceService
    ) {
        this.javaServerPersistenceService = javaServerPersistenceService;
        this.planPersistenceService = planPersistenceService;
    }

    @Override
    public boolean isType(SpecificationType type) {
        return type.equals(SpecificationType.JAVA_SERVER);
    }

    @Override
    public void handle(SubscriptionEntity subscription) {
        Optional<JavaServerEntity> javaServerEntity = javaServerPersistenceService.selectJavaServerFromSubscription(subscription.subscriptionId());
        if (javaServerEntity.isEmpty() && subscription.status().equals("active")) {
            this.create(subscription);
        } else {
            this.update(javaServerEntity.get(), subscription);
        }
        return;
    }

    public void update(JavaServerEntity javaServer, SubscriptionEntity subscription) {
        LOGGER.log(Level.INFO, String.format("Updating java server %s from subscription %s", javaServer.server_id(), subscription.subscriptionId()));
        return;
    }

    public void create(SubscriptionEntity subscription) {
        LOGGER.log(Level.INFO, String.format("Creating java server from subscription %s", subscription.subscriptionId()));
        String specification_id = planPersistenceService.selectSpecificationId(subscription.priceId())
            .orElseThrow(() -> new IllegalStateException("No spec associated with price " + subscription.priceId()));
        return;
    }
    
}
