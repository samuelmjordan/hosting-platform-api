package com.mc_host.api.service;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.mc_host.api.configuration.StripeConfiguration;
import com.stripe.model.Event;

@Service
public class StripeEventProcessor {
    private static final Logger LOGGER = Logger.getLogger(StripeEventProcessor.class.getName());

    private final StripeConfiguration stripeConfiguration;

    public StripeEventProcessor(
        StripeConfiguration stripeConfiguration
    ) {
        this.stripeConfiguration = stripeConfiguration;
    }
    
    @Async("webhookTaskExecutor")
    public CompletableFuture<Void> processEvent(Event event) {
        try {
            if (!stripeConfiguration.isAcceptibleEvent().test(event.getType())) {
                LOGGER.log(Level.WARNING, String.format(
                    "[Thread: %s] Not processing event %s, type %s is unsupported",
                    Thread.currentThread().getName(),
                    event.getId(),
                    event.getType()
                ));
                return CompletableFuture.completedFuture(null);
            }

                LOGGER.log(Level.INFO, String.format(
                    "[Thread: %s] Completed processing event %s, type %s",
                    Thread.currentThread().getName(),
                    event.getId(),
                    event.getType()
                ));
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format("Error processing event %s", event.getId()), e);
            return CompletableFuture.failedFuture(e);
        }
    }
}