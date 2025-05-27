package com.mc_host.api.service.stripe;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.MarketingRegion;
import com.mc_host.api.model.MetadataKey;
import com.mc_host.api.model.cache.StripeEventType;
import com.mc_host.api.model.entity.ContentSubscription;
import com.mc_host.api.model.entity.SubscriptionPair;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.repository.SubscriptionRepository;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.Mode;
import com.mc_host.api.service.resources.v2.context.Status;
import com.mc_host.api.service.resources.v2.context.StepType;
import com.mc_host.api.service.resources.v2.service.ServerExecutor;
import com.mc_host.api.util.Task;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;

@Service
public class StripeSubscriptionService implements StripeEventService {
    private static final Logger LOGGER = Logger.getLogger(StripeSubscriptionService.class.getName());

    private final ServerExecutor serverExecutor;
    private final SubscriptionRepository subscriptionRepository;
    private final ServerExecutionContextRepository serverExecutionContextRepository;

    public StripeSubscriptionService(
        ServerExecutor serverExecutor,
        SubscriptionRepository subscriptionRepository,
        ServerExecutionContextRepository serverExecutionContextRepository
    ) {
        this.serverExecutor = serverExecutor;
        this.subscriptionRepository = subscriptionRepository;
        this.serverExecutionContextRepository = serverExecutionContextRepository;
    }

    public StripeEventType getType() {
        return StripeEventType.SUBSCRIPTION;
    }

    public void process(String customerId) {
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
                        Context context = serverExecutionContextRepository.selectSubscription(subscription.subscriptionId())
                            .orElseThrow(() -> new IllegalStateException("Context not found for subscription: " + subscription.subscriptionId()));
                        serverExecutor.execute(context.inProgress().withMode(Mode.DESTROY));
                        subscriptionRepository.deleteSubscription(subscription.subscriptionId());
                    }
                )).toList();

            List<CompletableFuture<Void>> createTasks = subscriptionsToCreate.stream()
                .map(subscription -> Task.alwaysAttempt(
                    "Create subscription " + subscription.subscriptionId(),
                    () -> {
                        subscriptionRepository.insertSubscription(subscription);
                        serverExecutor.execute(
                            new Context(
                                subscription.subscriptionId(),
                                StepType.NEW,
                                Mode.CREATE,
                                Status.IN_PROGRESS
                            )
                        );
                    }
                )).toList();

            List<CompletableFuture<Void>> updateTasks = subscriptionsToUpdate.stream()
                .map(subscriptionPair -> Task.alwaysAttempt(
                    "Update subscription " + subscriptionPair.getOldSubscription().subscriptionId(),
                    () -> {
                        Context context = serverExecutionContextRepository.selectSubscription(subscriptionPair.getNewSubscription().subscriptionId())
                            .orElseThrow(() -> new IllegalStateException("Context not found for subscription: " + subscriptionPair.getNewSubscription().subscriptionId()));
                        subscriptionRepository.updateSubscription(subscriptionPair.getNewSubscription());
                        serverExecutor.execute(context.inProgress());
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
            subscription.getCancelAtPeriodEnd(),
            subscription.getMetadata().getOrDefault(MetadataKey.TITLE, "My Minecraft Server"),
            subscription.getMetadata().getOrDefault(MetadataKey.CAPTION, "Created " + LocalDateTime.now()),
            MarketingRegion.valueOf(subscription.getMetadata().getOrDefault(MetadataKey.REGION, MarketingRegion.WEST_EUROPE.name()))
        );
    }
}
