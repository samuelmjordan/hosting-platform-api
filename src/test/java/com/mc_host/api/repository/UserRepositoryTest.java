package com.mc_host.api.repository;

import com.mc_host.api.DatabaseTest;
import com.mc_host.api.model.plan.AcceptedCurrency;
import com.mc_host.api.model.user.ApplicationUser;
import com.mc_host.api.service.EncryptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
class UserRepositoryTest extends DatabaseTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EncryptionService encryptionService;

    @Test
    void insertUser_shouldInsertUserSuccessfully() {
        ApplicationUser user = createTestUser("test-clerk-id", "test-customer-id");

        int result = userRepository.insertUser(user);

        assertThat(result).isEqualTo(1);
        
        Optional<ApplicationUser> retrievedUser = userRepository.selectUser(user.clerkId());
        assertThat(retrievedUser).isPresent();
        assertThat(retrievedUser.get().clerkId()).isEqualTo(user.clerkId());
        assertThat(retrievedUser.get().customerId()).isEqualTo(user.customerId());
        assertThat(retrievedUser.get().pterodactylUsername()).isEqualTo(user.pterodactylUsername());
        assertThat(retrievedUser.get().pterodactylPassword()).isEqualTo(user.pterodactylPassword());
        assertThat(retrievedUser.get().primaryEmail()).isEqualTo(user.primaryEmail());
    }

    @Test
    void selectUser_shouldReturnEmptyWhenUserNotExists() {
        Optional<ApplicationUser> user = userRepository.selectUser("non-existent-id");

        assertThat(user).isEmpty();
    }

    @Test
    void selectUserByCustomerId_shouldReturnUser() {
        ApplicationUser user = createTestUser("test-clerk-2", "test-customer-2");
        userRepository.insertUser(user);

        Optional<ApplicationUser> retrievedUser = userRepository.selectUserByCustomerId(user.customerId());

        assertThat(retrievedUser).isPresent();
        assertThat(retrievedUser.get().customerId()).isEqualTo(user.customerId());
    }

    @Test
    void updatePrimaryEmail_shouldUpdateEmail() {
        ApplicationUser user = createTestUser("test-clerk-3", "test-customer-3");
        userRepository.insertUser(user);
        String newEmail = "updated@example.com";

        int result = userRepository.updatePrimaryEmail(newEmail, user.clerkId());

        assertThat(result).isEqualTo(1);
        Optional<ApplicationUser> updatedUser = userRepository.selectUser(user.clerkId());
        assertThat(updatedUser).isPresent();
        assertThat(updatedUser.get().primaryEmail()).isEqualTo(newEmail);
    }

    @Test
    void delete_shouldSoftDeleteUser() {
        ApplicationUser user = createTestUser("test-clerk-4", "test-customer-4");
        userRepository.insertUser(user);

        int result = userRepository.delete(user.clerkId());

        assertThat(result).isEqualTo(1);
        Optional<ApplicationUser> deletedUser = userRepository.selectUser(user.clerkId());
        assertThat(deletedUser).isEmpty();
    }

    @Test
    void usernameExists_shouldReturnTrueWhenExists() {
        ApplicationUser user = createTestUser("test-clerk-5", "test-customer-5");
        userRepository.insertUser(user);

        boolean exists = userRepository.usernameExists(user.pterodactylUsername());

        assertThat(exists).isTrue();
    }

    @Test
    void usernameExists_shouldReturnFalseWhenNotExists() {
        boolean exists = userRepository.usernameExists("non-existent-username");

        assertThat(exists).isFalse();
    }

    @Test
    void selectCustomerIdByClerkId_shouldReturnCustomerId() {
        ApplicationUser user = createTestUser("test-clerk-6", "test-customer-6");
        userRepository.insertUser(user);

        Optional<String> customerId = userRepository.selectCustomerIdByClerkId(user.clerkId());

        assertThat(customerId).isPresent();
        assertThat(customerId.get()).isEqualTo(user.customerId());
    }

    @Test
    void selectUserCurrency_shouldReturnDefaultWhenNotSet() {
        ApplicationUser user = createTestUser("test-clerk-7", "test-customer-7");
        userRepository.insertUser(user);

        Optional<AcceptedCurrency> currency = userRepository.selectUserCurrency(user.clerkId());

        assertThat(currency).isPresent();
        assertThat(currency.get()).isEqualTo(AcceptedCurrency.XXX);
    }

    @Test
    void passwordEncryption_shouldEncryptAndDecryptCorrectly() {
        String originalPassword = "my-secret-password";
        ApplicationUser user = new ApplicationUser(
            "test-clerk-8",
            "test-customer-8",
            12345L,
            "testuser8",
            originalPassword,
            "test8@example.com",
            "dummy8@example.com"
        );

        userRepository.insertUser(user);
        Optional<ApplicationUser> retrievedUser = userRepository.selectUser(user.clerkId());

        assertThat(retrievedUser).isPresent();
        assertThat(retrievedUser.get().pterodactylPassword()).isEqualTo(originalPassword);
    }

    private ApplicationUser createTestUser(String clerkId, String customerId) {
        return new ApplicationUser(
            clerkId,
            customerId,
            12345L,
            "testuser_" + clerkId,
            "test-password",
            clerkId + "@example.com",
            "dummy_" + clerkId + "@example.com"
        );
    }
}