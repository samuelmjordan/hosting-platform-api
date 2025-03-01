package com.mc_host.api.service.stripe;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.configuration.StripeConfiguration;
import com.mc_host.api.model.AcceptedCurrency;
import com.mc_host.api.model.cache.CacheNamespace;
import com.mc_host.api.model.cache.Queue;
import com.mc_host.api.model.entity.ContentPrice;
import com.mc_host.api.model.entity.PricePair;
import com.mc_host.api.model.specification.SpecificationType;
import com.mc_host.api.persistence.PriceRepository;
import com.mc_host.api.util.CacheService;
import com.mc_host.api.util.Task;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Price;
import com.stripe.model.StripeObject;
import com.stripe.param.PriceListParams;

@Service
public class StripeEventProcessor {
    private static final Logger LOGGER = Logger.getLogger(StripeEventProcessor.class.getName());

    private final StripeConfiguration stripeConfiguration;
    private final CacheService cacheService;
    private final PriceRepository priceRepository;
    private final ScheduledExecutorService delayedTaskScheduler;

    public StripeEventProcessor(
        StripeConfiguration stripeConfiguration,
        CacheService cacheService,
        PriceRepository priceRepository,
        ScheduledExecutorService delayedTaskScheduler
    ) {
        this.stripeConfiguration = stripeConfiguration;
        this.cacheService = cacheService;
        this.priceRepository = priceRepository;
        this. delayedTaskScheduler = delayedTaskScheduler;
    }
    
    public void processEvent(Event event) {
        try {
            if (!stripeConfiguration.isAcceptableEvent().test(event.getType())) {
                LOGGER.log(Level.WARNING, String.format(
                    "[Thread: %s] Discarding event %s, type %s is unsupported",
                    Thread.currentThread().getName(),
                    event.getId(),
                    event.getType()
                ));
                return;
            }

            if (stripeConfiguration.isSubscriptionEvent().test(event.getType())) {
                String customerId = extractValueFromEvent(event, "customer")
                    .orElseThrow(() -> new IllegalStateException(String.format("Failed to get customerId - eventType: %s", event.getType())));
                if (cacheService.flagIfAbsent(CacheNamespace.SUBSCRIPTION_SYNC_DEBOUNCE, customerId, Duration.ofMillis(stripeConfiguration.getEventDebounceTtlMs()))) {
                    delayedTaskScheduler.schedule(
                        () -> cacheService.queuePush(Queue.SUBSCRIPTION_SYNC, customerId),
                        stripeConfiguration.getEventDebounceTtlMs(), TimeUnit.MILLISECONDS);
                }
            }

            if (stripeConfiguration.isPriceEvent().test(event.getType())) {
                String productId = extractValueFromEvent(event, "product")
                    .orElseThrow(() -> new IllegalStateException(String.format("Failed to get productId - eventType: %s", event.getType())));
                syncPriceData(productId);
            }

            LOGGER.log(Level.INFO, String.format(
                "[Thread: %s] Pushed event %s, type %s",
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

    public void syncPriceData(String productId) {
        try {
            PriceListParams priceListParams = PriceListParams.builder()
                .setProduct(productId)
                .build();
            List<ContentPrice> stripePrices = Price.list(priceListParams).getData().stream()
                .map(price -> stripePriceToEntity(price, productId))
                .toList();
            List<ContentPrice> dbPrices = priceRepository.selectPricesByProductId(productId);
    
            List<ContentPrice> pricesToDelete = dbPrices.stream()
                .filter(dbPrice -> stripePrices.stream().noneMatch(dbPrice::isAlike))
                .toList();
            List<ContentPrice> pricesToCreate = stripePrices.stream()
                .filter(stripePrice -> dbPrices.stream().noneMatch(stripePrice::isAlike))
                .toList();
            List<PricePair> pricesToUpdate = dbPrices.stream()
                .flatMap(dbPrice -> stripePrices.stream()
                    .filter(dbPrice::isAlike)
                    .map(stripeSubscription -> new PricePair(dbPrice, stripeSubscription)))
                .toList();

            List<CompletableFuture<Void>> deleteTasks = pricesToDelete.stream()
                .map(price -> Task.alwaysAttempt(
                    "Delete price " + price.priceId(),
                    () -> priceRepository.deleteProductPrice(price.priceId(), productId)
                )).toList();

            List<CompletableFuture<Void>> createTasks = pricesToCreate.stream()
                .map(price -> Task.alwaysAttempt(
                    "Create price " + price.priceId(),
                    () -> priceRepository.insertPrice(price)
                )).toList();

            List<CompletableFuture<Void>> updateTasks = pricesToUpdate.stream()
                .map(pricePair -> Task.alwaysAttempt(
                    "Update price " + pricePair.getOldPrice().priceId(),
                    () -> priceRepository.insertPrice(pricePair.getNewPrice())
                )).toList();

            List<CompletableFuture<Void>> allTasks = new ArrayList<>();
            allTasks.addAll(deleteTasks);
            allTasks.addAll(createTasks);
            allTasks.addAll(updateTasks);
            Task.awaitCompletion(allTasks);
            
            cacheService.evict(CacheNamespace.SPECIFICATION_PLANS, SpecificationType.fromProductId(productId).name());
            LOGGER.log(Level.INFO, "Executed price db sync for product: " + productId);
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