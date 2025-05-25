package com.mc_host.api.queue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.cache.Queue;
import com.mc_host.api.service.stripe.StripePaymentMethodService;
import com.mc_host.api.util.Cache;

@Service
public class PaymentMethodConsumer extends AbstractQueueConsumer {
    private static final Logger LOGGER = Logger.getLogger(PaymentMethodConsumer.class.getName());
    
    private final StripePaymentMethodService stripePaymentMethodService;
    
    public PaymentMethodConsumer(
            ScheduledExecutorService scheduledExecutor,
            ExecutorService taskExecutor,
            Cache cacheService,
            StripePaymentMethodService stripePaymentMethodService) {
        super(scheduledExecutor, taskExecutor, cacheService);
        this.stripePaymentMethodService = stripePaymentMethodService;
    }
    
    @Override
    public Queue getQueue() {
        return Queue.PAYMENT_METHOD_SYNC;
    }
    
    @Override
    public void processItem(String customerId) {
        try {
            stripePaymentMethodService.sync(customerId);
            resetBackoff();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing customer " + customerId, e);
            applyBackoff();
            requeueItem(customerId);
            throw new RuntimeException("Failed to process payment methods for customer: " + customerId, e);
        }
    }
}