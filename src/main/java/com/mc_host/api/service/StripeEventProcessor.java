package com.mc_host.api.service;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.configuration.StripeConfiguration;
import com.mc_host.api.model.SubscriptionEntity;
import com.mc_host.api.persistence.SubscriptionPersistenceService;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;

@Service
public class StripeEventProcessor {
    private static final Logger LOGGER = Logger.getLogger(StripeEventProcessor.class.getName());

    private final StripeConfiguration stripeConfiguration;
    private final SubscriptionPersistenceService subscriptionPersistenceService;

    public StripeEventProcessor(
        StripeConfiguration stripeConfiguration,
        SubscriptionPersistenceService subscriptionPersistenceService
    ) {
        this.stripeConfiguration = stripeConfiguration;
        this.subscriptionPersistenceService = subscriptionPersistenceService;
    }
    
    @Async("webhookTaskExecutor")
    public CompletableFuture<Void> processEvent(Event event) {
        try {
            if (!stripeConfiguration.isAcceptibleEvent().test(event.getType())) {
                LOGGER.log(Level.WARNING, String.format(
                    "[Thread: %s] Not processing event %s, type %s is unsupported",
                    Thread.currentThread().getName(),
                    event.getId(),
                    event.getType()
                ));
                return CompletableFuture.completedFuture(null);
            }

            String customerId = extractCustomerId(event)
                .orElseThrow(() -> new IllegalStateException(String.format("Failed to get customerId - eventType: %s", event.getType())));
            syncSubscriptionData(customerId);
            LOGGER.log(Level.INFO, String.format(
                "[Thread: %s] Completed processing event %s, type %s",
                Thread.currentThread().getName(),
                event.getId(),
                event.getType()
            ));
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format(
                "Error processing event %s", 
                event.getId()), 
                e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public static Optional<String> extractCustomerId(Event event) {
        if (event == null || event.getData() == null || event.getData().getObject() == null) {
            return Optional.empty();
        }

        StripeObject eventObject = event.getData().getObject();
        
        // First try the direct approach (most common case)
        try {
            // Most Stripe objects have this method
            Method getCustomerMethod = eventObject.getClass().getMethod("getCustomer");
            Object result = getCustomerMethod.invoke(eventObject);
            if (result != null) {
                return Optional.of(result.toString());
            }
        } catch (Exception ignored) {
            // If this fails, we'll try the fallback below
        }
        
        // Fallback: convert to JSON and then to Map
        try {
            // Get the raw JSON representation
            String jsonString = eventObject.toJson();
            
            // Convert to Map
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> rawData = mapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
            
            // Try to get customer ID from the map
            Object customerId = rawData.get("customer");
            return customerId != null ? Optional.of(customerId.toString()) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Transactional
    private void syncSubscriptionData(String customerId) {
        try {
            List<SubscriptionEntity> dbSubscriptions = subscriptionPersistenceService.selectSubscriptionsByCustomerId(customerId);

            List<Subscription> stripeSubscriptions = Subscription.list(Map.of("customer", customerId)).getData();
            Set<String> stripeSubscriptionIds = stripeSubscriptions.stream().map(Subscription::getId).collect(Collectors.toSet());

            Set<String> dbSubscriptionsToDelete = dbSubscriptions.stream()
            .map(SubscriptionEntity::subscriptionId)
            .filter(id -> !stripeSubscriptionIds.contains(id))
            .collect(Collectors.toSet());

            if (!dbSubscriptionsToDelete.isEmpty())  {
                subscriptionPersistenceService.deleteCustomerSubscriptions(dbSubscriptionsToDelete, customerId);
            }

            stripeSubscriptions.stream()
            .map(subscription -> stripeToEntity(subscription, customerId))
            .forEach(subsription -> subscriptionPersistenceService.insertSubscription(subsription));

        } catch (StripeException e) {
            e.printStackTrace();
        }
    }

    private SubscriptionEntity stripeToEntity(Subscription subscription, String customerId) {
        return new SubscriptionEntity(
            subscription.getId(), 
            customerId, 
            subscription.getStatus(), 
            subscription.getItems().getData().getFirst().getPrice().getId(), 
            Instant.ofEpochMilli(subscription.getCurrentPeriodEnd()), 
            Instant.ofEpochMilli(subscription.getCurrentPeriodStart()), 
            subscription.getCancelAtPeriodEnd()
        );
    }
}