package com.mc_host.api.service;

import com.mc_host.api.repository.SubscriptionRepository;
import com.mc_host.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FakerServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private SubscriptionRepository subscriptionRepository;

    private FakerService fakerService;

    @BeforeEach
    void setUp() {
        fakerService = new FakerService(userRepository, subscriptionRepository);
    }

    @Test
    void generateUsername_shouldReturnValidUsername() {
        when(userRepository.usernameExists(anyString())).thenReturn(false);
        
        String username = fakerService.generateUsername();

        assertThat(username).isNotNull();
        assertThat(username).isNotEmpty();
        assertThat(username).matches("^[a-z0-9]+$");
        assertThat(username.length()).isBetween(5, 100);
    }

    @Test
    void generateUsername_shouldReturnUniqueUsernames() {
        when(userRepository.usernameExists(anyString())).thenReturn(false);
        
        String username1 = fakerService.generateUsername();
        String username2 = fakerService.generateUsername();

        assertThat(username1).isNotEqualTo(username2);
    }

    @Test
    void generateUsername_shouldRetryWhenUsernameExists() {
        when(userRepository.usernameExists(anyString()))
            .thenReturn(true, true, false); // First two attempts exist, third is available

        String username = fakerService.generateUsername();

        assertThat(username).isNotNull();
        assertThat(username).isNotEmpty();
    }

    @Test
    void generatePassword_shouldReturnValidPassword() {
        String password = fakerService.generatePassword();

        assertThat(password).isNotNull();
        assertThat(password.length()).isEqualTo(24);
        assertThat(password).matches("^[A-Za-z0-9!@#$%^&*()_+\\-=?]+$");
    }

    @Test
    void generatePassword_shouldReturnUniquePasswords() {
        String password1 = fakerService.generatePassword();
        String password2 = fakerService.generatePassword();

        assertThat(password1).isNotEqualTo(password2);
    }

    @Test
    void generateFirstname_shouldReturnValidFirstname() {
        String firstname = fakerService.generateFirstname();

        assertThat(firstname).isNotNull();
        assertThat(firstname).isNotEmpty();
        assertThat(firstname).matches("^[A-Za-z]+$");
    }

    @Test
    void generateLastname_shouldReturnValidLastname() {
        String lastname = fakerService.generateLastname();

        assertThat(lastname).isNotNull();
        assertThat(lastname).isNotEmpty();
        assertThat(lastname).matches("^[A-Za-z]+$");
    }

    @Test
    void generateSubdomain_shouldReturnValidSubdomain() {
        when(subscriptionRepository.domainExists(anyString())).thenReturn(false);
        
        String subdomain = fakerService.generateSubdomain();

        assertThat(subdomain).isNotNull();
        assertThat(subdomain).isNotEmpty();
        assertThat(subdomain).matches("^[a-z0-9\\-]+$");
        assertThat(subdomain.length()).isLessThan(58);
        assertThat(subdomain).contains("-");
    }

    @Test
    void generateSubdomain_shouldRetryWhenSubdomainExists() {
        when(subscriptionRepository.domainExists(anyString()))
            .thenReturn(true, false); // First attempt exists, second is available

        String subdomain = fakerService.generateSubdomain();

        assertThat(subdomain).isNotNull();
        assertThat(subdomain).isNotEmpty();
    }

    @Test
    void generateSubdomain_shouldRespectLengthLimit() {
        when(subscriptionRepository.domainExists(anyString())).thenReturn(false);
        
        String subdomain = fakerService.generateSubdomain();

        assertThat(subdomain.length()).isLessThan(58);
    }

    @Test
    void generateMultiplePasswords_shouldAllBeUnique() {
        var passwords = java.util.stream.IntStream.range(0, 10)
            .mapToObj(i -> fakerService.generatePassword())
            .toList();

        assertThat(passwords).hasSize(10);
        assertThat(passwords).doesNotHaveDuplicates();
    }

    @Test
    void generateMultipleUsernames_shouldAllBeUnique() {
        when(userRepository.usernameExists(anyString())).thenReturn(false);
        
        var usernames = java.util.stream.IntStream.range(0, 5)
            .mapToObj(i -> fakerService.generateUsername())
            .toList();

        assertThat(usernames).hasSize(5);
        assertThat(usernames).doesNotHaveDuplicates();
    }

    @Test
    void generateMultipleSubdomains_shouldAllBeUnique() {
        when(subscriptionRepository.domainExists(anyString())).thenReturn(false);
        
        var subdomains = java.util.stream.IntStream.range(0, 5)
            .mapToObj(i -> fakerService.generateSubdomain())
            .toList();

        assertThat(subdomains).hasSize(5);
        assertThat(subdomains).doesNotHaveDuplicates();
    }
}