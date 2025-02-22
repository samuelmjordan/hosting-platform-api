package com.mc_host.api.service.stripe;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
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
import com.mc_host.api.model.Currency;
import com.mc_host.api.model.entity.PriceEntity;
import com.mc_host.api.model.entity.SubscriptionEntity;
import com.mc_host.api.model.specification.SpecificationType;
import com.mc_host.api.persistence.PriceRepository;
import com.mc_host.api.persistence.SubscriptionRepository;
import com.mc_host.api.service.product.ProductServiceSupplier;
import com.mc_host.api.service.util.CachingService;
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
    private final CachingService cachingService;
    private final SubscriptionRepository subscriptionRepository;
    private final PriceRepository priceRepository;
    private final Executor virtualThreadExecutor;
    private final ScheduledExecutorService delayedTaskScheduler;

    public StripeEventProcessor(
        StripeConfiguration stripeConfiguration,
        ProductServiceSupplier productServiceSupplier,
        CachingService cachingService,
        SubscriptionRepository subscriptionRepository,
        PriceRepository priceRepository,
        Executor virtualThreadExecutor,
        ScheduledExecutorService delayedTaskScheduler
    ) {
        this.stripeConfiguration = stripeConfiguration;
        this.productServiceSupplier = productServiceSupplier;
        this.cachingService = cachingService;
        this.subscriptionRepository = subscriptionRepository;
        this.priceRepository = priceRepository;
        this.virtualThreadExecutor = virtualThreadExecutor;
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

    @Transactional
    private void syncSubscriptionData(String customerId) {
        try {
            List<SubscriptionEntity> dbSubscriptions = subscriptionRepository.selectSubscriptionsByCustomerId(customerId);

            List<Subscription> stripeSubscriptions = Subscription.list(Map.of("customer", customerId)).getData();
            Set<String> stripeSubscriptionIds = stripeSubscriptions.stream().map(Subscription::getId).collect(Collectors.toSet());

            Set<String> dbSubscriptionsToDelete = dbSubscriptions.stream()
                .map(SubscriptionEntity::subscriptionId)
                .filter(id -> !stripeSubscriptionIds.contains(id))
                .collect(Collectors.toSet());

            if (!dbSubscriptionsToDelete.isEmpty())  {
                subscriptionRepository.deleteCustomerSubscriptions(dbSubscriptionsToDelete, customerId);
            }

            List<CompletableFuture<Void>> futures = stripeSubscriptions.stream()
                .map(subscription -> stripeSubscriptionToEntity(subscription, customerId))
                .map(subscription -> CompletableFuture.runAsync(() -> {
                    try {
                        subscriptionRepository.insertSubscription(subscription);
                        productServiceSupplier.supplyAndHandle(subscription);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Sync failed for subscription id " + subscription.subscriptionId(), e);
                        throw e;
                    }
                }, virtualThreadExecutor))
                .toList();
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                
            subscriptionRepository.updateUserCurrencyFromSubscription(customerId);
            LOGGER.log(Level.INFO, "Executed subscription db sync for customer: " + customerId);
        } catch (StripeException e) {
            LOGGER.log(Level.SEVERE, "Failed to sync subscription data for customer: " + customerId, e);
            throw new RuntimeException("Failed to sync subscription data", e);
        }
    }

    private SubscriptionEntity stripeSubscriptionToEntity(Subscription subscription, String customerId) {
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

    @Transactional
    public List<PriceEntity> syncPriceData(String productId) {
        try {
            List<PriceEntity> dbPrices = priceRepository.selectPricesByProductId(productId);
    
            PriceListParams priceListParams = PriceListParams.builder()
                .setProduct(productId)
                .build();
            List<Price> stripePrices = Price.list(priceListParams).getData();
            Set<String> stripePriceIds = stripePrices.stream().map(Price::getId).collect(Collectors.toSet());
    
            Set<String> dbPricesToDelete = dbPrices.stream()
                .map(PriceEntity::priceId)
                .filter(id -> !stripePriceIds.contains(id))
                .collect(Collectors.toSet());
    
            if (!dbPricesToDelete.isEmpty())  {
                priceRepository.deleteProductPrices(dbPricesToDelete, productId);
            }
    
            List<PriceEntity> stripePriceEntities = stripePrices.stream()
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

    private PriceEntity stripePriceToEntity(Price price, String productId) {
        return new PriceEntity(
            price.getId(), 
            productId, 
            price.getActive(),
            Currency.fromCode(price.getCurrency()),
            price.getUnitAmount()
        );
    }
}