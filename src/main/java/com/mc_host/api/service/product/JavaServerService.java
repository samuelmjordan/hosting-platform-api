package com.mc_host.api.service.product;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.entity.SubscriptionEntity;
import com.mc_host.api.model.entity.server.JavaServer;
import com.mc_host.api.model.entity.server.ProvisioningState;
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
        Optional<JavaServer> javaServerEntity = javaServerPersistenceService.selectJavaServerFromSubscription(subscription.subscriptionId());
        if (javaServerEntity.isEmpty() && subscription.status().equals("active")) {
            this.create(subscription);
        } else {
            this.update(javaServerEntity.get(), subscription);
        }
        return;
    }

    public void update(JavaServer javaServer, SubscriptionEntity subscription) {
        LOGGER.log(Level.INFO, String.format("Updating java server %s from subscription %s", javaServer.getServerId(), subscription.subscriptionId()));
        return;
    }

    public void create(SubscriptionEntity subscription) {
        LOGGER.log(Level.INFO, String.format("Creating java server from subscription %s", subscription.subscriptionId()));

        try {        
            String planId = planPersistenceService.selectPlanIdFromPriceId(subscription.priceId())
                .orElseThrow(() -> new IllegalStateException("No spec associated with price " + subscription.priceId()));

            JavaServer javaServer = JavaServer.builder()
                .serverId(UUID.randomUUID().toString())
                .subscriptionId(subscription.subscriptionId())
                .planId(planId)
                .build();

            javaServerPersistenceService.insertNewJavaServer(javaServer);

            javaServer.getProvisioningState().validateTransition(ProvisioningState.METAL_PROVISIONED);
            // Provision metal
            javaServer.transitionState(ProvisioningState.METAL_PROVISIONED);

            javaServer.getProvisioningState().validateTransition(ProvisioningState.NODE_PROVISIONED);
            // Provision node
            javaServer.transitionState(ProvisioningState.NODE_PROVISIONED);

            javaServer.getProvisioningState().validateTransition(ProvisioningState.READY);
            // Not sure actually lol
            javaServer.transitionState(ProvisioningState.READY);

            LOGGER.log(Level.INFO, String.format("Java server %s %s", javaServer.getServerId(), javaServer.getProvisioningState()));
        } catch(Exception e) {
            // recovery logic
            throw e;
        }
    }
    
}
