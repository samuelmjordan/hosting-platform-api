package com.mc_host.api.service.clerk;

import com.clerk.backend_api.models.components.EmailAddress;
import com.clerk.backend_api.models.components.User;
import com.clerk.backend_api.models.operations.GetUserResponse;
import com.mc_host.api.client.PterodactylApplicationClient;
import com.mc_host.api.configuration.ApplicationConfiguration;
import com.mc_host.api.configuration.ClerkConfiguration;
import com.mc_host.api.model.user.ApplicationUser;
import com.mc_host.api.model.user.ClerkUserEvent;
import com.mc_host.api.repository.UserRepository;
import com.stripe.model.Customer;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ClerkEventProcessor {
    private static final Logger LOGGER = Logger.getLogger(ClerkEventProcessor.class.getName());
    private static final Faker FAKER = new Faker();

    private final UserRepository userRepository;
    private final ClerkConfiguration clerkConfiguration;
    private final ApplicationConfiguration applicationConfiguration;
    private final PterodactylApplicationClient pterodactylApplicationClient;

    public void processEvent(ClerkUserEvent event) {
        LOGGER.info(String.format("Processing %s %s", event.type(), event.userId()));

        switch (event.type()) {
            case "user.created":
            case "user.updated":
                syncUser(event.userId());
                break;
            case "user.deleted":
                deleteUser(event.userId());
                break;
            default:
                throw new IllegalStateException("Illegal clerk event type: " + event.type());
        }
    }

    public void syncUser(String clerkId) {
        Optional<ApplicationUser> applicationUser = userRepository.selectUser(clerkId);
        if (applicationUser.isPresent()) {
            updateUser(clerkId);
            return;
        }

        createUser(clerkId);
    }

    private void updateUser(String clerkId) {
        User user = null;
        try {
            user = getClerkUser(clerkId);
        } catch (ResponseStatusException e) {
            if (e.getStatusCode().value() == 404) {
                LOGGER.warning("User %s not found in clerk, initiating account deletion workflow.".formatted(clerkId));
                deleteUser(clerkId);
                return;
            }
            throw new RuntimeException("Clerk api error for user " + clerkId, e);
        } catch (Exception e) {
            throw new RuntimeException("Error syncing clerk user " + clerkId, e);
        }

        String primaryEmail = getPrimaryEmail(user);

        ApplicationUser applicationUser = userRepository.selectUser(clerkId)
            .orElseThrow(() -> new IllegalStateException("Cannot find user %s: invalid update flow".formatted(clerkId)));
        if (applicationUser.primaryEmail().equals(primaryEmail)) {
            return;
        }

        try {
            Customer stripeCustomer = Customer.retrieve(applicationUser.customerId());
            Map<String, Object> updateParams = Map.of("email", primaryEmail);
            stripeCustomer.update(updateParams);
            userRepository.updatePrimaryEmail(primaryEmail, clerkId);
            LOGGER.info("Updated stripe email for user: " + clerkId);
        } catch (Exception e) {
            throw new RuntimeException("Error updating stripe customer email for " + clerkId, e);
        }
    }

    private void createUser(String clerkId) {
        try {
            User user = getClerkUser(clerkId);
            String primaryEmail = getPrimaryEmail(user);

            String customerId = createNewStripeCustomer(clerkId, primaryEmail);

            String pterodactylUsername = generateUsername();
            String pterodactylPassword = generatePassword();
            String pterodactylDummyEmail = clerkId + "@" + applicationConfiguration.getDomain();
            Long pterodactylUserId = createNewPterodactylUser(
                clerkId, pterodactylDummyEmail, pterodactylUsername, pterodactylPassword);

            ApplicationUser newApplicationUser = new ApplicationUser(
                clerkId,
                customerId,
                pterodactylUserId,
                pterodactylUsername,
                pterodactylPassword,
                primaryEmail,
                pterodactylDummyEmail
            );

            userRepository.insertUser(newApplicationUser);
            LOGGER.info("New clerk user: " + clerkId);
        } catch (Exception e) {
            throw new RuntimeException("Error syncing clerk user " + clerkId, e);
        }
    }

    private User getClerkUser(String clerkId) throws Exception {
        GetUserResponse userResponse = clerkConfiguration.getClient().users().get()
            .userId(clerkId)
            .call();
        return userResponse.user()
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatusCode.valueOf(404), "Clerk user not found via api"));
    }

    private String getPrimaryEmail(User user) {
        JsonNullable<String> primaryEmailIdNullable = user.primaryEmailAddressId();
        if (!primaryEmailIdNullable.isPresent()) {
            throw new RuntimeException("No primary email ID set");
        }
        String primaryEmailId = primaryEmailIdNullable.get();
        return user.emailAddresses()
            .orElseThrow(() -> new RuntimeException("No email addresses found"))
            .stream()
            .filter(email -> email.id().isPresent() && email.id().get().equals(primaryEmailId))
            .map(EmailAddress::emailAddress)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Primary email address not found in list of user emails"));
    }

    private String generateUsername() {
        String username;
		do {
			username = Stream.of(
                    FAKER.word().adjective(),
                    FAKER.color().name(),
                    FAKER.animal().name(),
                    FAKER.word().verb(),
                    String.valueOf(FAKER.number().numberBetween(10, 99)))
                .reduce(String::concat).get()
                .replace(" ", "")
                .toLowerCase();
		} while (userRepository.usernameExists(username));
        return username;
    }

    private String generatePassword() {
        SecureRandom random = new SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-=?";

        return random.ints(24, 0, chars.length())
            .mapToObj(chars::charAt)
            .map(Object::toString)
            .collect(Collectors.joining());
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
            throw new RuntimeException("Error creating stripe customer", e);
        }
    }

    private Long createNewPterodactylUser(String clerkId, String email, String username, String password) {
        try {
            PterodactylApplicationClient.UserAttributes userAttributes = pterodactylApplicationClient.createUser(
                new PterodactylApplicationClient.PterodactylCreateUserRequest(
                    email,
                    username,
                    FAKER.name().firstName(),
                    FAKER.name().lastName(),
                    "en",
                    password
            )).attributes();
            return userAttributes.id();
        } catch (Exception e) {
            throw new RuntimeException("Error creating pterodactyl customer", e);
        }
    }

    private void deleteUser(String clerkId) {
        LOGGER.info("Delete clerk user: " + clerkId);
        // delete all accounts
    }
    
}
