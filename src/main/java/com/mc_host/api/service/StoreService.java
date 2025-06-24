package com.mc_host.api.service;

import com.mc_host.api.controller.api.StoreController;
import com.mc_host.api.model.plan.AcceptedCurrency;
import com.mc_host.api.model.stripe.request.CheckoutRequest;
import com.mc_host.api.model.user.ClerkUserEvent;
import com.mc_host.api.repository.GameServerSpecRepository;
import com.mc_host.api.service.clerk.ClerkEventProcessor;
import com.mc_host.api.service.data.DataFetchingService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class StoreService implements StoreController {
	private static final Logger LOGGER = Logger.getLogger(StoreService.class.getName());

	private final GameServerSpecRepository gameServerSpecRepository;
	private final ClerkEventProcessor clerkEventProcessor;
	private final DataFetchingService dataFetchingService;
	private final Executor virtualThreadExecutor;


	@Override
	public ResponseEntity<String> startCheckout(
		String clerkId,
		CheckoutRequest request
	) {
		try {
			LOGGER.log(Level.INFO, "Starting checkout creation for clerkId: " + clerkId);

			String customerId = getOrCreateCustomerId(clerkId);

			String priceId = getPriceInCorrectCurrency(request.priceId(), clerkId);
			if (!priceId.equals(request.priceId())) {
				LOGGER.log(Level.WARNING,
					String.format("User attempted checkout with wrong currency. userId: %s  priceID: %s",
						clerkId, request.priceId()));
			}

			SessionCreateParams checkoutParams = SessionCreateParams.builder()
				.setMode(SessionCreateParams.Mode.SUBSCRIPTION)
				.setCustomer(customerId)
				.setSuccessUrl(request.success())
				.setCancelUrl(request.cancel())
				.setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
					.build())
				.addLineItem(SessionCreateParams.LineItem.builder()
					.setPrice(priceId)
					.setQuantity(1L)
					.build())
				.build();

			Session session = Session.create(checkoutParams);

			LOGGER.log(Level.INFO, "Complete checkout creation for clerkId: " + clerkId);
			return ResponseEntity.ok(session.getUrl());

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
		AcceptedCurrency currency = dataFetchingService.getUserCurrencyInner(userId);
		if (currency.equals(AcceptedCurrency.XXX)) {
			return priceId;
		}
		return gameServerSpecRepository.convertPrice(priceId, currency)
			.orElseThrow(() -> new IllegalStateException(String.format("No alternative for priceId %s in %s", priceId, currency)));
	}

	public String getOrCreateCustomerId(String userId) {
		Optional<String> customerId = dataFetchingService.getUserCustomerId(userId);
		if (customerId.isEmpty()) {
			virtualThreadExecutor.execute(() -> clerkEventProcessor.processEvent(new ClerkUserEvent("user.created", userId)));
			throw new IllegalStateException("Failed to find user: " + userId);
		}

		return customerId.get();
	}
}
