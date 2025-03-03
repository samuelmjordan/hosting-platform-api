package com.mc_host.api.service.stripe;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.entity.ContentSubscription;
import com.mc_host.api.model.entity.SubscriptionPair;
import com.mc_host.api.persistence.SubscriptionRepository;
import com.mc_host.api.service.product.SubscriptionServiceSupplier;
import com.mc_host.api.util.Task;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;

@Service
public class StripeSubscriptionService {
    private static final Logger LOGGER = Logger.getLogger(StripeSubscriptionService.class.getName());

    private final SubscriptionServiceSupplier productServiceSupplier;
    private final SubscriptionRepository subscriptionRepository;

    public StripeSubscriptionService(
        SubscriptionServiceSupplier productServiceSupplier,
        SubscriptionRepository subscriptionRepository
    ) {
        this.productServiceSupplier = productServiceSupplier;
        this.subscriptionRepository = subscriptionRepository;
    }

    public void handleCustomerSubscriptionSync(String customerId) {
        try {
            List<ContentSubscription> dbSubscriptions = subscriptionRepository.selectSubscriptionsByCustomerId(customerId);
            List<ContentSubscription> stripeSubscriptions = Subscription.list(Map.of("customer", customerId)).getData().stream()
                .map(subscription -> stripeSubscriptionToEntity(subscription, customerId)).toList();

            List<ContentSubscription> subscriptionsToDelete = dbSubscriptions.stream()
                .filter(dbSubscription -> stripeSubscriptions.stream().noneMatch(dbSubscription::isAlike))
                .toList();
            List<ContentSubscription> subscriptionsToCreate = stripeSubscriptions.stream()
                .filter(stripeSubscription -> dbSubscriptions.stream().noneMatch(stripeSubscription::isAlike))
                .toList();
            List<SubscriptionPair> subscriptionsToUpdate = dbSubscriptions.stream()
                .flatMap(dbSubscription -> stripeSubscriptions.stream()
                    .filter(dbSubscription::isAlike)
                    .map(stripeSubscription -> new SubscriptionPair(dbSubscription, stripeSubscription)))
                .toList();

            List<CompletableFuture<Void>> deleteTasks = subscriptionsToDelete.stream()
                .map(subscription -> Task.alwaysAttempt(
                    "Delete subscription " + subscription.subscriptionId(),
                    () -> {
                        productServiceSupplier.supply(subscription).delete(subscription);
                        subscriptionRepository.deleteCustomerSubscription(subscription.subscriptionId(), customerId);
                    }
                )).toList();

            List<CompletableFuture<Void>> createTasks = subscriptionsToCreate.stream()
                .map(subscription -> Task.alwaysAttempt(
                    "Create subscription " + subscription.subscriptionId(),
                    () -> {
                        subscriptionRepository.insertSubscription(subscription);
                        productServiceSupplier.supply(subscription).create(subscription);
                    }
                )).toList();

            List<CompletableFuture<Void>> updateTasks = subscriptionsToUpdate.stream()
                .map(subscriptionPair -> Task.alwaysAttempt(
                    "Update subscription " + subscriptionPair.getOldSubscription().subscriptionId(),
                    () -> {
                        subscriptionRepository.insertSubscription(subscriptionPair.getNewSubscription());
                        productServiceSupplier.supply(subscriptionPair.getNewSubscription())
                            .update(subscriptionPair.getOldSubscription(), subscriptionPair.getNewSubscription());
                    }
                )).toList();

            List<CompletableFuture<Void>> allTasks = new ArrayList<>();
            allTasks.addAll(deleteTasks);
            allTasks.addAll(createTasks);
            allTasks.addAll(updateTasks);
            Task.awaitCompletion(allTasks);
                
            CompletableFuture<Void> updateCurrency = Task.criticalTask(
                "Update user currency for " + customerId,
                () -> subscriptionRepository.updateUserCurrencyFromSubscription(customerId)
            );
            
            Task.awaitCompletion(updateCurrency);
            LOGGER.log(Level.INFO, "Executed subscription db sync for customer: " + customerId);
        } catch (StripeException e) {
            LOGGER.log(Level.SEVERE, "Failed to sync subscription data for customer: " + customerId, e);
            throw new RuntimeException("Failed to sync subscription data", e);
        }
    }

    private ContentSubscription stripeSubscriptionToEntity(Subscription subscription, String customerId) {
        return new ContentSubscription(
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
