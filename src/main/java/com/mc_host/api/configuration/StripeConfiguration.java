package com.mc_host.api.configuration;

import java.util.List;
import java.util.function.Predicate;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.stripe.Stripe;

import jakarta.annotation.PostConstruct;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "stripe")
public class StripeConfiguration {
    private String apiKey;
    private String signingKey;
    private String activeJavaProductId;
    private Long eventDebounceTtlMs;
    private Long subscriptionSyncTimeoutMinutes;
    private List<String> subscriptionEvents;
    private List<String> priceEvents;
    private List<String> invoiceEvents;
    private List<String> paymentMethodEvents;

    @PostConstruct
    public void init() {
        Stripe.apiKey = this.getApiKey();
    }

    public Predicate<String> isSubscriptionEvent() {
        return (eventType) -> subscriptionEvents.contains(eventType);
    }

    public Predicate<String> isPriceEvent() {
        return (eventType) -> priceEvents.contains(eventType);
    }

    public Predicate<String> isInvoiceEvent() {
        return (eventType) -> invoiceEvents.contains(eventType);
    }

    public Predicate<String> isPaymentMethodEvent() {
        return (eventType) -> paymentMethodEvents.contains(eventType);
    }

    public Predicate<String> isAcceptableEvent() {
        return (eventType) -> isSubscriptionEvent()
            .or(isPriceEvent())
            .or(isInvoiceEvent())
            .or(isPaymentMethodEvent())
            .test(eventType);
    }
}
