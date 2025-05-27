package com.mc_host.api.service.stripe;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.configuration.StripeConfiguration;
import com.mc_host.api.model.cache.CacheNamespace;
import com.mc_host.api.model.cache.Queue;
import com.mc_host.api.model.cache.StripeEventType;
import com.mc_host.api.util.Cache;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;

@Service
public class StripeEventProcessor {
    private static final Logger LOGGER = Logger.getLogger(StripeEventProcessor.class.getName());
    private static final Queue QUEUE_NAME = Queue.STRIPE_EVENT;

    private final StripeConfiguration stripeConfiguration;
    private final Cache cacheService;
    private final ScheduledExecutorService delayedTaskScheduler;

    private final Map<Predicate<String>, EventConfig> eventHandlers;

    private record EventConfig(StripeEventType type, CacheNamespace debounceFlag, String fieldName) {}

    public StripeEventProcessor(
        StripeConfiguration stripeConfiguration,
        Cache cacheService,
        ScheduledExecutorService delayedTaskScheduler
    ) {
        this.stripeConfiguration = stripeConfiguration;
        this.cacheService = cacheService;
        this.delayedTaskScheduler = delayedTaskScheduler;
        
        this.eventHandlers = Map.of(
            stripeConfiguration.isInvoiceEvent(), new EventConfig(StripeEventType.INVOICE, CacheNamespace.INVOICE_DEBOUNCE, "customer"),
            stripeConfiguration.isSubscriptionEvent(), new EventConfig(StripeEventType.SUBSCRIPTION, CacheNamespace.SUBSCRIPTION_DEBOUNCE, "customer"),
            stripeConfiguration.isPriceEvent(), new EventConfig(StripeEventType.PRICE, CacheNamespace.PRICE_DEBOUNCE, "product"),
            stripeConfiguration.isPaymentMethodEvent(), new EventConfig(StripeEventType.PAYMENT_METHOD, CacheNamespace.PAYMENT_METHOD_DEBOUNCE, "customer")
        );
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

            eventHandlers.entrySet().stream()
                .filter(entry -> entry.getKey().test(event.getType()))
                .findFirst()
                .ifPresent(entry -> {
                    EventConfig config = entry.getValue();
                    String extractedId = extractValueFromEvent(event, config.fieldName())
                        .orElseThrow(() -> new IllegalStateException(
                            String.format("Failed to get %s - eventType: %s", config.fieldName(), event.getType())
                        ));
                    queuePushDebounce(config, extractedId);
                });

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

    public void queuePushDebounce(EventConfig eventConfig, String flag) {
        if (cacheService.flagIfAbsent(
                eventConfig.debounceFlag(),
                flag,
                Duration.ofMillis(stripeConfiguration.getEventDebounceTtlMs())
            )
        ) {
            delayedTaskScheduler.schedule(
                () -> cacheService.queueLeftPush(QUEUE_NAME, String.join(":", eventConfig.type().name(), flag)),
                stripeConfiguration.getEventDebounceTtlMs(), 
                TimeUnit.MILLISECONDS
            );
        }
    }
}