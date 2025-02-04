package com.mc_host.api.configuration;

import java.util.List;
import java.util.function.Predicate;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.stripe.Stripe;

import jakarta.annotation.PostConstruct;

@Configuration
@ConfigurationProperties(prefix = "stripe")
public class StripeConfiguration {
    private String apiKey;
    private String signingKey;
    private List<String> acceptableEvents;

    @PostConstruct
    public void setStripeApiKey() {
        Stripe.apiKey = this.getApiKey();
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSigningKey() {
        return signingKey;
    }

    public void setSigningKey(String webhookSigning) {
        this.signingKey = webhookSigning;
    }

    public List<String> getAcceptableEvents() {
        return acceptableEvents;
    }

    public void setAcceptableEvents(List<String> webhookEvents) {
        this.acceptableEvents = webhookEvents;
    }

    public Predicate<String> isAcceptibleEvent() {
        return (eventType) -> acceptableEvents.contains(eventType);
    }
}
