package com.mc_host.api.service.stripe;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.mc_host.api.controller.PaymentMethodResource;
import com.mc_host.api.exceptions.CustomerNotFoundException;
import com.mc_host.api.model.AcceptedCurrency;
import com.mc_host.api.model.request.CreatePaymentMethodRequest;
import com.mc_host.api.service.data.DataFetchingService;
import com.stripe.exception.StripeException;

@Service
public class PaymentMethodService implements PaymentMethodResource{
    private static final Logger LOGGER = Logger.getLogger(PaymentMethodService.class.getName());

    private final StripeService stripeService;
    private final DataFetchingService dataFetchingService;

    public PaymentMethodService(
        StripeService stripeService,
        DataFetchingService dataFetchingService
    ) {
        this.stripeService = stripeService;
        this.dataFetchingService = dataFetchingService;
    }

    @Override
    public ResponseEntity<Void> setDefaultPaymentMethod(String userId, String paymentMethodId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setDefaultPaymentMethod'");
    }

    @Override
    public ResponseEntity<Void> editPaymentMethod(String userId, String paymentMethodId) {
        throw new UnsupportedOperationException("Unimplemented method 'editPaymentMethod'");
    }

    @Override
    public ResponseEntity<Void> removePaymentMethod(String userId, String paymentMethodId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removePaymentMethod'");
    }

    @Override
    public ResponseEntity<String> createPymentMethod(String userId, CreatePaymentMethodRequest request) {
        try {
            String customerId = stripeService.getCustomerId(userId);
            AcceptedCurrency currency = dataFetchingService.getUserCurrencyInner(userId);
            if (currency.equals(AcceptedCurrency.XXX)) {
                currency = request.currency();
            }
            
            com.stripe.param.checkout.SessionCreateParams params = com.stripe.param.checkout.SessionCreateParams.builder()
                .setMode(com.stripe.param.checkout.SessionCreateParams.Mode.SETUP)
                .setCustomer(customerId)
                .setSuccessUrl(request.success())
                .setCancelUrl(request.cancel())
                .setCurrency(currency.name())
                .build();
            com.stripe.model.checkout.Session portalSession = com.stripe.model.checkout.Session.create(params);
            LOGGER.log(Level.INFO, "Complete create payment method portal creation for clerkId: " + userId);
            return ResponseEntity.ok(portalSession.getUrl());
        } catch (CustomerNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Failed to find or create customer", e);
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .build();
        } catch (StripeException e) {
            LOGGER.log(Level.SEVERE, "Stripe API error during create payment method portal creation", e);
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
        }
    }
    
}
