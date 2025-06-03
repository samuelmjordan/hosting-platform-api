package com.mc_host.api.service.stripe;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.mc_host.api.configuration.StripeConfiguration;
import com.mc_host.api.controller.StripeResource;
import com.mc_host.api.exceptions.CustomerNotFoundException;
import com.mc_host.api.model.cache.CacheNamespace;
import com.mc_host.api.model.plan.AcceptedCurrency;
import com.mc_host.api.model.stripe.MetadataKey;
import com.mc_host.api.model.stripe.request.CheckoutRequest;
import com.mc_host.api.model.stripe.request.PortalRequest;
import com.mc_host.api.model.user.ClerkUserEvent;
import com.mc_host.api.repository.GameServerSpecRepository;
import com.mc_host.api.service.clerk.ClerkEventProcessor;
import com.mc_host.api.service.data.DataFetchingService;
import com.mc_host.api.service.stripe.events.StripeEventProcessor;
import com.mc_host.api.util.Cache;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;

@Service
public class StripeService implements StripeResource {
    private static final Logger LOGGER = Logger.getLogger(StripeService.class.getName());
    
    private final Cache cacheService;
    private final StripeConfiguration stripeConfiguration;
    private final ClerkEventProcessor clerkEventProcessor;
    private final StripeEventProcessor stripeEventProcessor;
    private final GameServerSpecRepository gameServerSpecRepository;
    private final DataFetchingService dataFetchingService;
    private final Executor virtualThreadExecutor;

    public StripeService(
        Cache cacheService,
        StripeConfiguration stripeConfiguration,
        StripeEventProcessor eventProcessor,
        ClerkEventProcessor clerkEventProcessor,
        GameServerSpecRepository gameServerSpecRepository,
        DataFetchingService dataFetchingService,
        Executor virtualThreadExecutor
    ) {
        this.cacheService = cacheService;
        this.stripeConfiguration = stripeConfiguration;
        this.stripeEventProcessor = eventProcessor;
        this.clerkEventProcessor = clerkEventProcessor;
        this.gameServerSpecRepository = gameServerSpecRepository;
        this.dataFetchingService  = dataFetchingService;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    @Override
    public ResponseEntity<String> handleStripeWebhook(String payload, String sigHeader) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, stripeConfiguration.getSigningKey());
            LOGGER.log(Level.INFO, String.format(
                "[Thread: %s] Stripe Event Received - Type: %s, ID: %s",
                Thread.currentThread().getName(),
                event.getType(),
                event.getId()
            ));
            virtualThreadExecutor.execute(() -> stripeEventProcessor.processEvent(event));
            return ResponseEntity.ok().body("Webhook Received");
        } catch (SignatureVerificationException e) {
            LOGGER.log(Level.SEVERE, "Invalid signature", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,"Unexpected error processing webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
        }
    }

    @Override
    public ResponseEntity<String> startCheckout(CheckoutRequest request) {
        try {
            LOGGER.log(Level.INFO, "Starting checkout creation for clerkId: " + request.userId());

            String customerId = getCustomerId(request.userId());

            String priceId = getPriceInCorrectCurrency(request.priceId(), request.userId());
            if (!priceId.equals(request.priceId())) {
                LOGGER.log(Level.WARNING, 
                String.format("User attempted checkout with wrong currency. userId: %s  priceID: %s",
                request.userId(), request.priceId()));
            }
    
            SessionCreateParams checkoutParams = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(customerId)
                .setSuccessUrl(request.success())
                .setCancelUrl(request.cancel())
                .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                    .putMetadata(MetadataKey.REGION.name(), request.region().name())
                    .build()) 
                .addLineItem(SessionCreateParams.LineItem.builder()
                    .setPrice(priceId)
                    .setQuantity(1L)
                    .build())
                .build();

            Session session = Session.create(checkoutParams);

            LOGGER.log(Level.INFO, "Complete checkout creation for clerkId: " + request.userId());
            return ResponseEntity.ok(session.getUrl());
    
        } catch (CustomerNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Failed to find or create customer", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Unable to find or create customer profile");
        } catch (StripeException e) {
            LOGGER.log(Level.SEVERE, "Stripe API error during checkout creation", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Unable to create checkout session");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during checkout creation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An unexpected error occurred");
        }
    }

    @Override
    public ResponseEntity<String> userPortal(PortalRequest request) {
        try {
            String customerId = getCustomerId(request.userId());
            com.stripe.param.billingportal.SessionCreateParams params = com.stripe.param.billingportal.SessionCreateParams.builder()
                .setCustomer(customerId)
                .setReturnUrl(request.returnUrl())
                .build();
            com.stripe.model.billingportal.Session portalSession = com.stripe.model.billingportal.Session.create(params);
            LOGGER.log(Level.INFO, "Complete portal creation for clerkId: " + request.userId());
            return ResponseEntity.ok(portalSession.getUrl());
        } catch (CustomerNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Failed to find or create customer", e);
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .build();
        } catch (StripeException e) {
            LOGGER.log(Level.SEVERE, "Stripe API error during portal creation", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        }
    }

    private String getPriceInCorrectCurrency(String priceId, String userId) {
        cacheService.evict(CacheNamespace.USER_CURRENCY, userId);
        AcceptedCurrency currency = dataFetchingService.getUserCurrencyInner(userId);
        if (currency.equals(AcceptedCurrency.XXX)) {
            return priceId;
        }
        return gameServerSpecRepository.convertPrice(priceId, currency)
            .orElseThrow(() -> new IllegalStateException(String.format("No alternative for priceId %s in %s", priceId, currency)));
    }

    public String getCustomerId(String userId) throws CustomerNotFoundException {
        Optional<String> customerId = dataFetchingService.getUserCustomerId(userId);
        if (customerId.isEmpty()) {
            virtualThreadExecutor.execute(() -> clerkEventProcessor.processEvent(new ClerkUserEvent("user.created", userId)));
            new IllegalStateException("Failed to find user: " + userId);
        }

        return customerId.get();
    }

    @Override
    public ResponseEntity<Void> cancelSubscription(String userId, String subscriptionId) {
        return updateCancelAtPeriodEnd(userId, subscriptionId, true);
    }

    @Override
    public ResponseEntity<Void> uncancelSubscription(String userId, String subscriptionId) {
        return updateCancelAtPeriodEnd(userId, subscriptionId, false);
    }

    private ResponseEntity<Void> updateCancelAtPeriodEnd(String userId, String subscriptionId, Boolean cancel) {
        try {
            String customerId = getCustomerId(userId);
            
            Subscription subscription = Subscription.retrieve(subscriptionId);
            if (!subscription.getCustomer().equals(customerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                .setCancelAtPeriodEnd(cancel)
                .build();
            subscription.update(params);
            
            return ResponseEntity.ok().build();
        } catch (CustomerNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Failed to find or create customer", e);
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .build();
        } catch (StripeException e) {
            LOGGER.log(Level.SEVERE, "Stripe API error during subscription update", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        }
    }
}