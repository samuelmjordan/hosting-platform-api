package com.mc_host.api.service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.clerk.backend_api.models.components.User;
import com.clerk.backend_api.models.errors.ClerkErrors;
import com.clerk.backend_api.models.operations.GetUserResponse;
import com.mc_host.api.configuration.ClerkConfiguration;
import com.mc_host.api.configuration.StripeConfiguration;
import com.mc_host.api.controller.StripeResource;
import com.mc_host.api.exceptions.ClerkException;
import com.mc_host.api.exceptions.CustomerNotFoundException;
import com.mc_host.api.model.Currency;
import com.mc_host.api.model.entity.UserEntity;
import com.mc_host.api.model.request.CheckoutRequest;
import com.mc_host.api.persistence.JavaServerSpecPersistenceService;
import com.mc_host.api.persistence.UserPersistenceService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;

@Service
public class StripeService implements StripeResource{
    private static final Logger LOGGER = Logger.getLogger(StripeService.class.getName());
    
    private final StripeConfiguration stripeConfiguration;
    private final ClerkConfiguration clerkConfiguration;
    
    private final StripeEventProcessor stripeEventProcessor;
    private final UserPersistenceService userPersistenceService;
    private final JavaServerSpecPersistenceService javaServerSpecPersistenceService;
    private final DataFetchingService dataFetchingService;

    public StripeService(
        StripeConfiguration stripeConfiguration,
        ClerkConfiguration clerkConfiguration,
        StripeEventProcessor eventProcessor,
        UserPersistenceService userPersistenceService,
        JavaServerSpecPersistenceService javaServerSpecPersistenceService,
        DataFetchingService dataFetchingService
    ) {
        this.stripeConfiguration = stripeConfiguration;
        this.clerkConfiguration = clerkConfiguration;
        this.stripeEventProcessor = eventProcessor;
        this.userPersistenceService = userPersistenceService;
        this.javaServerSpecPersistenceService = javaServerSpecPersistenceService;
        this.dataFetchingService  = dataFetchingService;
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
            stripeEventProcessor.processEvent(event);
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

    private String getPriceInCorrectCurrency(String priceId, String userId) {
        Currency currency = dataFetchingService.getUserCurrencyInner(userId);
        if (currency.equals(Currency.XXX)) {
            return priceId;
        }
        return javaServerSpecPersistenceService.convertPrice(priceId, currency)
            .orElseThrow(() -> new IllegalStateException(String.format("No alternative for priceId %s in %s", priceId, currency)));
    }

    private String getCustomerId(String userId) throws CustomerNotFoundException {
        Optional<String> stripeCustomerId = userPersistenceService.selectCustomerIdByClerkId(userId);
        if (stripeCustomerId.isPresent()) {
            LOGGER.log(Level.INFO, "Completed fetching details - clerkId:  " + userId);
            return stripeCustomerId.get();
        }

        try {
            LOGGER.log(Level.INFO, "Creating new customer - clerkId: " + userId);
            String customerId = createNewStripeCustomer(userId);
            LOGGER.log(Level.INFO, "Completed creating new customer - clerkId: " + userId);
            return customerId;
        } catch (ClerkErrors | ClerkException e) {
            LOGGER.log(Level.SEVERE, "Clerk API error for clerkId: " + userId, e);
            throw new CustomerNotFoundException("Failed to create customer due to Clerk API error", e);
        } catch (StripeException e) {
            LOGGER.log(Level.SEVERE, "Stripe API error for clerkId: " + userId, e);
            throw new CustomerNotFoundException("Failed to create customer due to Stripe API error", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error for clerkId: " + userId, e);
            throw new CustomerNotFoundException("Failed to create customer due to unexpected error", e);
        }
    }

    private String createNewStripeCustomer(String userId) throws ClerkErrors, ClerkException, StripeException, Exception {
        GetUserResponse userResponse = clerkConfiguration.getClient().users().get()
            .userId(userId)
            .call();

        User user = userResponse.user()
            .orElseThrow(() -> new ClerkException("User not found"));

        JsonNullable<String> primaryEmailIdNullable = user.primaryEmailAddressId();
        if (!primaryEmailIdNullable.isPresent()) {
            throw new ClerkException("No primary email ID set");
        }
        String primaryEmailId = primaryEmailIdNullable.get();

        String primaryEmail = user.emailAddresses()
            .orElseThrow(() -> new ClerkException("No email addresses found"))
            .stream()
            .filter(email -> email.id().isPresent() && email.id().get().equals(primaryEmailId))
            .map(email -> email.emailAddress())
            .findFirst()
            .orElseThrow(() -> new ClerkException("Primary email address not found in list of user emails"));

        Map<String, Object> customerParams = Map.of(
            "email", primaryEmail,
            "metadata", Collections.singletonMap("clerk_id", userId)
        );
        
        Customer stripeCustomer = Customer.create(customerParams);
        userPersistenceService.insertUser(new UserEntity(userId, stripeCustomer.getId()));

        LOGGER.log(Level.INFO, "Created new stripe customerId - clerkId: " + userId);
        return stripeCustomer.getId();
    }
}