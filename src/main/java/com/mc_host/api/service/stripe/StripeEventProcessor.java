package com.mc_host.api.service.stripe;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.configuration.StripeConfiguration;
import com.mc_host.api.model.cache.CacheNamespace;
import com.mc_host.api.model.cache.Queue;
import com.mc_host.api.util.CacheService;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;

@Service
public class StripeEventProcessor {
    private static final Logger LOGGER = Logger.getLogger(StripeEventProcessor.class.getName());
    private static final Boolean HIGH_PRIORITY = true;
    private static final Boolean LOW_PRIORITY = false;

    private final StripeConfiguration stripeConfiguration;
    private final CacheService cacheService;
    private final ScheduledExecutorService delayedTaskScheduler;

    public StripeEventProcessor(
        StripeConfiguration stripeConfiguration,
        CacheService cacheService,
        ScheduledExecutorService delayedTaskScheduler
    ) {
        this.stripeConfiguration = stripeConfiguration;
        this.cacheService = cacheService;
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
                queuePushDebounce(Queue.SUBSCRIPTION_SYNC, CacheNamespace.SUBSCRIPTION_SYNC_DEBOUNCE, LOW_PRIORITY, customerId);
            }

            if (stripeConfiguration.isPriceEvent().test(event.getType())) {
                String productId = extractValueFromEvent(event, "product")
                    .orElseThrow(() -> new IllegalStateException(String.format("Failed to get productId - eventType: %s", event.getType())));
                queuePushDebounce(Queue.PRICE_SYNC, CacheNamespace.PRICE_SYNC_DEBOUNCE, HIGH_PRIORITY, productId);
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

    public void queuePushDebounce(Queue queue, CacheNamespace debounceFlag, Boolean priority, String value) {
        if (cacheService.flagIfAbsent(
            debounceFlag,
            value,
            Duration.ofMillis(stripeConfiguration.getEventDebounceTtlMs()))) {
        delayedTaskScheduler.schedule(
            priority
            ? () -> cacheService.queueRightPush(queue, value)
            : () -> cacheService.queueLeftPush(queue, value),
            stripeConfiguration.getEventDebounceTtlMs(), TimeUnit.MILLISECONDS);
        }
    }
}