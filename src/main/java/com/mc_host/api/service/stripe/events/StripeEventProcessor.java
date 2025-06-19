package com.mc_host.api.service.stripe.events;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.configuration.StripeConfiguration;
import com.mc_host.api.model.stripe.StripeEventType;
import com.mc_host.api.queuev2.model.JobType;
import com.mc_host.api.queuev2.service.JobScheduler;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class StripeEventProcessor {
    private static final Logger LOGGER = Logger.getLogger(StripeEventProcessor.class.getName());

    private final StripeConfiguration stripeConfiguration;
    private final JobScheduler jobScheduler;
    private final Map<StripeEventType, EventConfig> eventConfigs;

    private record EventConfig(
        JobType jobType,
        String extractionField,
        Predicate<String> eventTypePredicate
    ) {}

    public StripeEventProcessor(
        StripeConfiguration stripeConfiguration,
        JobScheduler jobScheduler
    ) {
        this.stripeConfiguration = stripeConfiguration;
        this.jobScheduler = jobScheduler;
        this.eventConfigs = Map.of(
            StripeEventType.INVOICE, new EventConfig(
                JobType.CUSTOMER_INVOICE_SYNC,
                "customer",
                stripeConfiguration.isInvoiceEvent()
            ),
            StripeEventType.SUBSCRIPTION, new EventConfig(
                JobType.CUSTOMER_SUBSCRIPTION_SYNC,
                "customer",
                stripeConfiguration.isSubscriptionEvent()
            ),
            StripeEventType.PRICE, new EventConfig(
                JobType.PRODUCT_PRICE_SYNC,
                "product",
                stripeConfiguration.isPriceEvent()
            ),
            StripeEventType.PAYMENT_METHOD, new EventConfig(
                JobType.CUSTOMER_PAYMENT_METHOD_SYNC,
                "customer",
                stripeConfiguration.isPaymentMethodEvent()
            )
        );
    }
    
    public void processEvent(Event event) {
        try {
            if (!stripeConfiguration.isAcceptableEvent().test(event.getType())) {
                LOGGER.log(Level.FINE, String.format(
                    "[Thread: %s] discarding event %s, type %s is unsupported",
                    Thread.currentThread().getName(),
                    event.getId(),
                    event.getType()
                ));
                return;
            }

            eventConfigs.entrySet().stream()
                .filter(entry -> entry.getValue().eventTypePredicate.test(event.getType()))
                .forEach(entry -> {
                    StripeEventType eventType = entry.getKey();
                    EventConfig config = entry.getValue();
                    String extractedId = extractValueFromEvent(event, config.extractionField)
                        .orElseThrow(() -> new IllegalStateException(
                            String.format("failed to extract %s from event %s (type: %s)", 
                                config.extractionField, event.getId(), event.getType())
                        ));
                    jobScheduler.schedule(config.jobType(), extractedId);
                });

            LOGGER.log(Level.FINE, String.format(
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

}