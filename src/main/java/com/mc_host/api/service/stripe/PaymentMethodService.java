package com.mc_host.api.service.stripe;

import com.mc_host.api.controller.PaymentMethodResource;
import com.mc_host.api.model.plan.AcceptedCurrency;
import com.mc_host.api.model.stripe.request.CreatePaymentMethodRequest;
import com.mc_host.api.queue.JobScheduler;
import com.mc_host.api.service.data.DataFetchingService;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.param.CustomerUpdateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class PaymentMethodService implements PaymentMethodResource{
    private static final Logger LOGGER = Logger.getLogger(PaymentMethodService.class.getName());

    private final StripeService stripeService;
    private final DataFetchingService dataFetchingService;
    private final JobScheduler jobScheduler;

    @Override
    public ResponseEntity<Void> removeDefaultPaymentMethod(String userId, String paymentMethodId) {
        try {
            String customerId = stripeService.getCustomerId(userId);
            CustomerUpdateParams params = CustomerUpdateParams.builder()
                .setInvoiceSettings(
                    CustomerUpdateParams.InvoiceSettings.builder()
                        .setDefaultPaymentMethod("")
                        .build())
                .build();

            Customer.retrieve(customerId).update(params);
            jobScheduler.schedulePaymentMethodSync(customerId);
            
            return ResponseEntity.ok().build();
        } catch (StripeException e) {
            LOGGER.log(Level.SEVERE, "Stripe API error during default payment method removal", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @Override
    public ResponseEntity<Void> setDefaultPaymentMethod(String userId, String paymentMethodId) {
        try {
            String customerId = stripeService.getCustomerId(userId);
            CustomerUpdateParams params = CustomerUpdateParams.builder()
                .setInvoiceSettings(
                    CustomerUpdateParams.InvoiceSettings.builder()
                        .setDefaultPaymentMethod(paymentMethodId)
                        .build())
                .build();
                
            Customer.retrieve(customerId).update(params);
            jobScheduler.schedulePaymentMethodSync(customerId);
            
            return ResponseEntity.ok().build();
        } catch (StripeException e) {
            LOGGER.log(Level.SEVERE, "Stripe API error during default payment method update", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @Override
    public ResponseEntity<Void> removePaymentMethod(String userId, String paymentMethodId) {
        String customerId = null;
        try {
            customerId = stripeService.getCustomerId(userId);
            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
    
            paymentMethod.detach();
            return ResponseEntity.ok().build();
        } catch (StripeException e) {
            LOGGER.log(Level.SEVERE, "Stripe API error during payment method removal", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } finally {
            if (customerId != null) {
                jobScheduler.schedulePaymentMethodSync(customerId);
            }
        }
    }

    @Override
    public ResponseEntity<String> createPaymentMethod(String userId, CreatePaymentMethodRequest request) {
        try {
            String customerId = stripeService.getCustomerId(userId);
            AcceptedCurrency currency = dataFetchingService.getUserCurrencyInner(userId);
            if (currency.equals(AcceptedCurrency.XXX)) {
                currency = AcceptedCurrency.EUR;
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
        } catch (StripeException e) {
            LOGGER.log(Level.SEVERE, "Stripe API error during create payment method portal creation", e);
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
        }
    }
    
}
