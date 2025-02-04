package com.mc_host.api.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.clerk.backend_api.Clerk;
import com.clerk.backend_api.models.components.EmailAddress;
import com.clerk.backend_api.models.operations.GetUserResponse;
import com.mc_host.api.configuration.ClerkConfiguration;
import com.mc_host.api.configuration.StripeConfiguration;
import com.mc_host.api.controller.StripeResource;
import com.mc_host.api.model.UserEntity;
import com.mc_host.api.persistence.UserPersistenceService;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.net.Webhook;

@Service
public class StripeService implements StripeResource{
    private static final Logger LOGGER = Logger.getLogger(StripeService.class.getName());
    
    private final StripeConfiguration stripeConfiguration;
    private final ClerkConfiguration clerkConfiguration;
    private final StripeEventProcessor stripeEventProcessor;
    private final UserPersistenceService userPersistenceService;

    public StripeService(
        StripeConfiguration stripeConfiguration,
        ClerkConfiguration clerkConfiguration,
        StripeEventProcessor eventProcessor,
        UserPersistenceService userPersistenceService
    ) {
        this.stripeConfiguration = stripeConfiguration;
        this.clerkConfiguration = clerkConfiguration;
        this.stripeEventProcessor = eventProcessor;
        this.userPersistenceService = userPersistenceService;
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
            LOGGER.log(Level.SEVERE,"Error processing webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook processing failed");
        }
    }

    @Override
    public ResponseEntity<String> getCustomerId(String userId) {
        LOGGER.log(Level.INFO, String.format(
            "getCustomerID Request Received - clerkId: %s",
            userId
        ));

        Optional<String> stripeCustomerId = userPersistenceService.selectCustomerIdByClerkId(userId);

        if (stripeCustomerId.isPresent()) {
            LOGGER.log(Level.INFO, String.format("Completed fetching details for clerk userId %s", userId));
            return ResponseEntity.ok().body(stripeCustomerId.get());
        }

        try {
            Clerk clerkSdk = Clerk.builder()
                .bearerAuth(clerkConfiguration.getKey())
                .build();

            GetUserResponse userResponse = clerkSdk.users().get()
                    .userId(userId)
                    .call();

            String primaryEmail = userResponse.user()
                .flatMap(user -> user.emailAddresses())
                .flatMap(emails -> emails.stream().findFirst())
                .map(EmailAddress::emailAddress)
                .orElseThrow(() -> new IllegalStateException(
                    String.format("No email address found for clerk userId %s", userId)
                ));

            Map<String, Object> customerParams = Map.of(
                "email", primaryEmail,
                "metadata", Collections.singletonMap("clerk_id", userId)
            );
    
            Stripe.apiKey = stripeConfiguration.getApiKey();
            Customer stripeCustomer = Customer.create(customerParams);
            userPersistenceService.insertUser(new UserEntity(userId, stripeCustomer.getId()));
    
            LOGGER.log(Level.INFO, String.format("Created new stripe customerId for clerk userId %s", userId));
            return ResponseEntity.ok().body(stripeCustomer.getId());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format("Error fetching details for clerk userId %s", userId), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Request failed");
        }
    }
}