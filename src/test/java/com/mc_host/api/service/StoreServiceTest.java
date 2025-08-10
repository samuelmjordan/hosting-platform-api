package com.mc_host.api.service;

import com.mc_host.api.model.plan.AcceptedCurrency;
import com.mc_host.api.model.stripe.request.CheckoutRequest;
import com.mc_host.api.model.user.ClerkUserEvent;
import com.mc_host.api.repository.GameServerSpecRepository;
import com.mc_host.api.service.clerk.ClerkEventProcessor;
import com.mc_host.api.service.data.UserService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private GameServerSpecRepository gameServerSpecRepository;
    @Mock
    private ClerkEventProcessor clerkEventProcessor;
    @Mock
    private UserService userService;
    @Mock
    private Executor virtualThreadExecutor;

    private StoreService storeService;

    @BeforeEach
    void setUp() {
        storeService = new StoreService(
            gameServerSpecRepository,
            clerkEventProcessor,
            userService,
            virtualThreadExecutor
        );
    }

    @Test
    void startCheckout_shouldCreateCheckoutSession() throws StripeException {
        String clerkId = "user_test123";
        String customerId = "cus_test123";
        CheckoutRequest request = new CheckoutRequest(
            "price_test123",
            AcceptedCurrency.USD,
            "https://example.com/success",
            "https://example.com/cancel"
        );

        when(userService.getUserCustomerId(clerkId)).thenReturn(Optional.of(customerId));

        try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
            Session mockSession = mock(Session.class);
            when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/session123");
            sessionMock.when(() -> Session.create(any(com.stripe.param.checkout.SessionCreateParams.class))).thenReturn(mockSession);

            ResponseEntity<String> response = storeService.startCheckout(clerkId, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo("https://checkout.stripe.com/session123");
        }
    }

    @Test
    void startCheckout_shouldHandleStripeException() throws StripeException {
        String clerkId = "user_test123";
        String customerId = "cus_test123";
        CheckoutRequest request = new CheckoutRequest(
            "price_test123",
            AcceptedCurrency.USD,
            "https://example.com/success",
            "https://example.com/cancel"
        );

        when(userService.getUserCustomerId(clerkId)).thenReturn(Optional.of(customerId));

        try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
            sessionMock.when(() -> Session.create(any(com.stripe.param.checkout.SessionCreateParams.class)))
                .thenThrow(new StripeException("Stripe API error", "request_id", "code", 400) {});

            ResponseEntity<String> response = storeService.startCheckout(clerkId, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(response.getBody()).contains("Unable to create checkout session");
        }
    }

    @Test
    void startCheckout_shouldHandleUnexpectedException() {
        String clerkId = "user_test123";
        CheckoutRequest request = new CheckoutRequest(
            "price_test123",
            AcceptedCurrency.USD,
            "https://example.com/success",
            "https://example.com/cancel"
        );

        when(userService.getUserCustomerId(clerkId)).thenThrow(new RuntimeException("Unexpected error"));

        ResponseEntity<String> response = storeService.startCheckout(clerkId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).contains("An unexpected error occurred");
    }

    @Test
    void getOrCreateCustomerId_shouldReturnExistingCustomerId() {
        String userId = "user_test123";
        String customerId = "cus_test123";
        when(userService.getUserCustomerId(userId)).thenReturn(Optional.of(customerId));

        String result = storeService.getOrCreateCustomerId(userId);

        assertThat(result).isEqualTo(customerId);
    }

    @Test
    void getOrCreateCustomerId_shouldTriggerUserCreationWhenNotFound() {
        String userId = "user_test123";
        when(userService.getUserCustomerId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storeService.getOrCreateCustomerId(userId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to find user: " + userId);

        verify(virtualThreadExecutor).execute(any(Runnable.class));
    }

    @Test
    void getOrCreateCustomerId_shouldProcessClerkUserCreatedEvent() {
        String userId = "user_test123";
        when(userService.getUserCustomerId(userId)).thenReturn(Optional.empty());

        try {
            storeService.getOrCreateCustomerId(userId);
        } catch (IllegalStateException e) {
            // Expected
        }

        verify(virtualThreadExecutor).execute(any(Runnable.class));
        
    }
}