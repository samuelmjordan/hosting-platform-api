package com.mc_host.api.service.stripe;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.configuration.StripeConfiguration;
import com.mc_host.api.model.CacheNamespace;
import com.mc_host.api.model.AcceptedCurrency;
import com.mc_host.api.model.entity.ContentPrice;
import com.mc_host.api.model.entity.ContentSubscription;
import com.mc_host.api.model.entity.SubscriptionPair;
import com.mc_host.api.model.specification.SpecificationType;
import com.mc_host.api.persistence.PriceRepository;
import com.mc_host.api.persistence.SubscriptionRepository;
import com.mc_host.api.service.product.ProductServiceSupplier;
import com.mc_host.api.util.Cache;
import com.mc_host.api.util.Task;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Price;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.param.PriceListParams;

@Service
public class StripeEventProcessor {
    private static final Logger LOGGER = Logger.getLogger(StripeEventProcessor.class.getName());

    private final StripeConfiguration stripeConfiguration;
    private final ProductServiceSupplier productServiceSupplier;
    private final Cache cachingService;
    private final SubscriptionRepository subscriptionRepository;
    private final PriceRepository priceRepository;
    private final ScheduledExecutorService delayedTaskScheduler;

    public StripeEventProcessor(
        StripeConfiguration stripeConfiguration,
        ProductServiceSupplier productServiceSupplier,
        Cache cachingService,
        SubscriptionRepository subscriptionRepository,
        PriceRepository priceRepository,
        ScheduledExecutorService delayedTaskScheduler
    ) {
        this.stripeConfiguration = stripeConfiguration;
        this.productServiceSupplier = productServiceSupplier;
        this.cachingService = cachingService;
        this.subscriptionRepository = subscriptionRepository;
        this.priceRepository = priceRepository;
        this. delayedTaskScheduler = delayedTaskScheduler;
    }
    
    public void processEvent(Event event) {
        try {
            if (!stripeConfiguration.isAcceptableEvent().test(event.getType())) {
                LOGGER.log(Level.WARNING, String.format(
                    "[Thread: %s] Not processing event %s, type %s is unsupported",
                    Thread.currentThread().getName(),
                    event.getId(),
                    event.getType()
                ));
                return;
            }

            if (stripeConfiguration.isSubscriptionEvent().test(event.getType())) {
                String customerId = extractValueFromEvent(event, "customer")
                    .orElseThrow(() -> new IllegalStateException(String.format("Failed to get customerId - eventType: %s", event.getType())));
                if (cachingService.flagIfAbsent(CacheNamespace.SUBSCRIPTION_SYNC, customerId, Duration.ofMillis(stripeConfiguration.getEventDebounceTtlMs()))) {
                    delayedTaskScheduler.schedule(() -> syncSubscriptionData(customerId), stripeConfiguration.getEventDebounceTtlMs(), TimeUnit.MILLISECONDS); 
                }
            }

            if (stripeConfiguration.isPriceEvent().test(event.getType())) {
                String productId = extractValueFromEvent(event, "product")
                    .orElseThrow(() -> new IllegalStateException(String.format("Failed to get productId - eventType: %s", event.getType())));
                syncPriceData(productId);
            }

            LOGGER.log(Level.INFO, String.format(
                "[Thread: %s] Completed processing event %s, type %s",
                Thread.currentThread().getName(),
                event.getId(),
                event.getType()
            ));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format(
                "Error processing event %s", 
                event.getId()), 
                e);
            throw e;
        }
    }

    @SuppressWarnings("deprecation")
    public static Optional<String> extractValueFromEvent(Event event, String valueName) {
        if (event == null || event.getData() == null || event.getData().getObject() == null) {
            return Optional.empty();
        }

        StripeObject eventObject = event.getData().getObject();
        
        try {
            String jsonString = eventObject.toJson();

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> rawData = mapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
            Object value = rawData.get(valueName);
            return value != null ? Optional.of(value.toString()) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

private void syncSubscriptionData(String customerId) {
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

        // Combine all tasks
        List<CompletableFuture<Void>> allTasks = new ArrayList<>();
        allTasks.addAll(deleteTasks);
        allTasks.addAll(createTasks);
        allTasks.addAll(updateTasks);
        Task.awaitCompletion(allTasks);
            
        // Final update after all operations complete
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

    @Transactional
    public List<ContentPrice> syncPriceData(String productId) {
        try {
            List<ContentPrice> dbPrices = priceRepository.selectPricesByProductId(productId);
    
            PriceListParams priceListParams = PriceListParams.builder()
                .setProduct(productId)
                .build();
            List<Price> stripePrices = Price.list(priceListParams).getData();
            Set<String> stripePriceIds = stripePrices.stream().map(Price::getId).collect(Collectors.toSet());
    
            Set<String> dbPricesToDelete = dbPrices.stream()
                .map(ContentPrice::priceId)
                .filter(id -> !stripePriceIds.contains(id))
                .collect(Collectors.toSet());
    
            if (!dbPricesToDelete.isEmpty())  {
                priceRepository.deleteProductPrices(dbPricesToDelete, productId);
            }
    
            List<ContentPrice> stripePriceEntities = stripePrices.stream()
                .map(price -> stripePriceToEntity(price, productId))
                .toList();
            cachingService.evict(CacheNamespace.SPECIFICATION_PLANS, SpecificationType.fromProductId(productId).name());
    
            stripePriceEntities.stream()
                .forEach(price-> priceRepository.insertPrice(price));
    
            return stripePriceEntities;
        } catch (StripeException e) {
            LOGGER.log(Level.SEVERE, "Failed to sync price data for product: " + productId, e);
            throw new RuntimeException("Failed to sync subscription data", e);
        }
    }

    private ContentPrice stripePriceToEntity(Price price, String productId) {
        return new ContentPrice(
            price.getId(), 
            productId, 
            price.getActive(),
            AcceptedCurrency.fromCode(price.getCurrency()),
            price.getUnitAmount()
        );
    }
}