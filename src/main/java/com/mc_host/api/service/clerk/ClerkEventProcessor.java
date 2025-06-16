package com.mc_host.api.service.clerk;

import com.clerk.backend_api.models.components.User;
import com.clerk.backend_api.models.operations.GetUserResponse;
import com.mc_host.api.configuration.ClerkConfiguration;
import com.mc_host.api.model.user.ApplicationUser;
import com.mc_host.api.model.user.ClerkUserEvent;
import com.mc_host.api.repository.UserRepository;
import com.stripe.model.Customer;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class ClerkEventProcessor {
    private static final Logger LOGGER = Logger.getLogger(ClerkEventProcessor.class.getName());

    private final UserRepository userRepository;
    private final ClerkConfiguration clerkConfiguration;

    public ClerkEventProcessor(
        UserRepository userRepository,
        ClerkConfiguration clerkConfiguration
    ) {
        this.userRepository = userRepository;
        this.clerkConfiguration = clerkConfiguration;
    }

    public void processEvent(ClerkUserEvent event) {
        LOGGER.info(String.format("Processing %s %s", event.type(), event.userId()));

        switch (event.type()) {
            case "user.created":
            case "user.updated":
                syncUser(event);
                break;
            case "user.deleted":
                deleteUser(event);
                break;
            default:
                throw new IllegalStateException("Illegal clerk event type: " + event.type());
        }

    }

    private void syncUser(ClerkUserEvent event) {
        String clerkId = event.userId();
        Optional<ApplicationUser> applicationUser = userRepository.selectUser(clerkId);
        if (applicationUser.isPresent()) {
            // update logic
            return;
        }

        try {
            GetUserResponse userResponse = clerkConfiguration.getClient().users().get()
                .userId(clerkId)
                .call();
            User user = userResponse.user()
                .orElseThrow(() -> new RuntimeException("User not found"));

            JsonNullable<String> primaryEmailIdNullable = user.primaryEmailAddressId();
            if (!primaryEmailIdNullable.isPresent()) {
                throw new RuntimeException("No primary email ID set");
            }
            String primaryEmailId = primaryEmailIdNullable.get();
            String primaryEmail = user.emailAddresses()
                .orElseThrow(() -> new RuntimeException("No email addresses found"))
                .stream()
                .filter(email -> email.id().isPresent() && email.id().get().equals(primaryEmailId))
                .map(email -> email.emailAddress())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Primary email address not found in list of user emails"));

            String customerId = createNewStripeCustomer(clerkId, primaryEmail);

            ApplicationUser newApplicationUser = new ApplicationUser(
                clerkId,
                customerId
            );

            userRepository.insertUser(newApplicationUser);
            LOGGER.info("New clerk user: " + event.userId());
        } catch (Exception e) {
            throw new RuntimeException("Error syncing clerk user " + clerkId);
        }
    }

    private String createNewStripeCustomer(String clerkId, String primaryEmail) {
        try {
            Map<String, Object> customerParams = Map.of(
                "email", primaryEmail,
                "metadata", Collections.singletonMap("clerk_id", clerkId)
            );
            Customer stripeCustomer = Customer.create(customerParams);
            return stripeCustomer.getId();        
        } catch (Exception e) {
            throw new RuntimeException("Error creating stripe customer");
        }
    }

    private void deleteUser(ClerkUserEvent event) {
        LOGGER.info("Delete clerk user: " + event.userId());
        // delete all accounts
    }
    
}
