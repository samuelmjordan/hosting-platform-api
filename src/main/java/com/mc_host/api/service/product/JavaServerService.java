package com.mc_host.api.service.product;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.client.HetznerClient;
import com.mc_host.api.model.entity.SubscriptionEntity;
import com.mc_host.api.model.entity.server.JavaServer;
import com.mc_host.api.model.entity.server.ProvisioningState;
import com.mc_host.api.model.specification.SpecificationType;
import com.mc_host.api.persistence.JavaServerPersistenceService;
import com.mc_host.api.persistence.PlanPersistenceService;

@Service
public class JavaServerService implements ProductService {
    private static final Logger LOGGER = Logger.getLogger(JavaServerService.class.getName());

    private final JavaServerPersistenceService javaServerPersistenceService;
    private final PlanPersistenceService planPersistenceService;
    private final HetznerClient hetznerClient;

    JavaServerService(
        JavaServerPersistenceService javaServerPersistenceService,
        PlanPersistenceService planPersistenceService,
        HetznerClient hetznerClient
    ) {
        this.javaServerPersistenceService = javaServerPersistenceService;
        this.planPersistenceService = planPersistenceService;
        this.hetznerClient = hetznerClient;
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

        JavaServer javaServer = null;
        try {        
            String planId = planPersistenceService.selectPlanIdFromPriceId(subscription.priceId())
                .orElseThrow(() -> new IllegalStateException("No spec associated with price " + subscription.priceId()));

            javaServer = JavaServer.builder()
                .serverId(UUID.randomUUID().toString())
                .subscriptionId(subscription.subscriptionId())
                .planId(planId)
                .build();

            javaServerPersistenceService.insertNewJavaServer(javaServer);

            // Start metal
            javaServer.getProvisioningState().validateTransition(ProvisioningState.METAL_PROVISIONED);
            String hetznerId = String.valueOf(hetznerClient.createServer(javaServer.getServerId(), "cax11", "nbg1", "ubuntu-24.04").server.id);
            javaServer.setHetznerId(hetznerId);
            if (!hetznerClient.waitForServerStatus(javaServer.getHetznerId(), "running")) {
                hetznerClient.deleteServer(Long.parseLong(javaServer.getHetznerId()));
                javaServer.setHetznerId(null);
                javaServer.transitionState(ProvisioningState.NEW);
                javaServerPersistenceService.updateJavaServer(javaServer);
                throw new RuntimeException(String.format("Server for subscription %s failed to reach running state within timeout", subscription.subscriptionId()));
            }
            javaServer.transitionState(ProvisioningState.METAL_PROVISIONED);
            javaServerPersistenceService.updateJavaServer(javaServer);

            // Start pterodactyl server
            javaServer.getProvisioningState().validateTransition(ProvisioningState.NODE_PROVISIONED);
            //
            javaServer.transitionState(ProvisioningState.NODE_PROVISIONED);
            javaServerPersistenceService.updateJavaServer(javaServer);

            javaServer.getProvisioningState().validateTransition(ProvisioningState.READY);
            //
            javaServer.transitionState(ProvisioningState.READY);
            javaServer.resetRetryCount();
            javaServerPersistenceService.updateJavaServer(javaServer);

            LOGGER.log(Level.INFO, String.format("Java server %s %s", javaServer.getServerId(), javaServer.getProvisioningState()));
        } catch(Exception e) {
            if (javaServer == null) {
                LOGGER.log(Level.SEVERE, String.format("Failure to create java server from subscription %s", subscription.subscriptionId()), e);
            }

            javaServer.incrementRetryCount();
            if (javaServer.getRetryCount() >= 3) {
                LOGGER.log(Level.SEVERE, String.format("Java server %s has attempted maximum retries. CRITICAL FAILURE. %s", javaServer.getServerId(), javaServer), e);
            }

            LOGGER.log(Level.SEVERE, String.format("Java server %s has failed. Attempt: %s", javaServer.getServerId(), javaServer.getRetryCount()), e);
            javaServer.incrementRetryCount();
            javaServerPersistenceService.updateJavaServer(javaServer);
        }
    }
    
}
