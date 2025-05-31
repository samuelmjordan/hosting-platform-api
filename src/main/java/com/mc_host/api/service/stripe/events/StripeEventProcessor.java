package com.mc_host.api.service.stripe.events;

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

    private final Map<StripeEventType, EventConfig> eventConfigs;

    private record EventConfig(
        CacheNamespace debounceFlag, 
        String extractionField,
        Predicate<String> eventTypePredicate
    ) {}

    public StripeEventProcessor(
        StripeConfiguration stripeConfiguration,
        Cache cacheService,
        ScheduledExecutorService delayedTaskScheduler
    ) {
        this.stripeConfiguration = stripeConfiguration;
        this.cacheService = cacheService;
        this.delayedTaskScheduler = delayedTaskScheduler;
        
        this.eventConfigs = Map.of(
            StripeEventType.INVOICE, new EventConfig(
                CacheNamespace.INVOICE_DEBOUNCE, 
                "customer",
                stripeConfiguration.isInvoiceEvent()
            ),
            StripeEventType.SUBSCRIPTION, new EventConfig(
                CacheNamespace.SUBSCRIPTION_DEBOUNCE, 
                "customer",
                stripeConfiguration.isSubscriptionEvent()
            ),
            StripeEventType.PRICE, new EventConfig(
                CacheNamespace.PRICE_DEBOUNCE, 
                "product",
                stripeConfiguration.isPriceEvent()
            ),
            StripeEventType.PAYMENT_METHOD, new EventConfig(
                CacheNamespace.PAYMENT_METHOD_DEBOUNCE, 
                "customer",
                stripeConfiguration.isPaymentMethodEvent()
            )
        );
    }

    public void enqueueEvent(StripeEventType eventType, String entityId) {
        EventConfig config = eventConfigs.get(eventType);
        if (config == null) {
            throw new IllegalArgumentException("unsupported event type: " + eventType);
        }
        
        queuePushDebounce(eventType, config, entityId);
        
        LOGGER.log(Level.INFO, String.format(
            "[Thread: %s] manually enqueued %s event for %s: %s",
            Thread.currentThread().getName(),
            eventType,
            config.extractionField,
            entityId
        ));
    }
    
    public void processEvent(Event event) {
        try {
            if (!stripeConfiguration.isAcceptableEvent().test(event.getType())) {
                LOGGER.log(Level.WARNING, String.format(
                    "[Thread: %s] discarding event %s, type %s is unsupported",
                    Thread.currentThread().getName(),
                    event.getId(),
                    event.getType()
                ));
                return;
            }

            eventConfigs.entrySet().stream()
                .filter(entry -> entry.getValue().eventTypePredicate.test(event.getType()))
                .findFirst()
                .ifPresent(entry -> {
                    StripeEventType eventType = entry.getKey();
                    EventConfig config = entry.getValue();
                    String extractedId = extractValueFromEvent(event, config.extractionField)
                        .orElseThrow(() -> new IllegalStateException(
                            String.format("failed to extract %s from event %s (type: %s)", 
                                config.extractionField, event.getId(), event.getType())
                        ));
                    queuePushDebounce(eventType, config, extractedId);
                });

            LOGGER.log(Level.INFO, String.format(
                "[Thread: %s] processed event %s (type: %s)",
                Thread.currentThread().getName(),
                event.getId(),
                event.getType()
            ));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format(
                "error processing event %s", 
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

    private void queuePushDebounce(StripeEventType eventType, EventConfig config, String entityId) {
        if (cacheService.flagIfAbsent(
                config.debounceFlag,
                entityId,
                Duration.ofMillis(stripeConfiguration.getEventDebounceTtlMs())
            )
        ) {
            delayedTaskScheduler.schedule(
                () -> cacheService.queueLeftPush(QUEUE_NAME, 
                    String.join(":", eventType.name(), entityId)),
                stripeConfiguration.getEventDebounceTtlMs(), 
                TimeUnit.MILLISECONDS
            );
        }
    }
}