package com.mc_host.api.queue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.cache.Queue;
import com.mc_host.api.service.stripe.StripeInvoiceService;
import com.mc_host.api.util.Cache;

@Service
public class InvoiceConsumer extends AbstractQueueConsumer {
    private static final Logger LOGGER = Logger.getLogger(InvoiceConsumer.class.getName());
    
    private final StripeInvoiceService stripeInvoiceService;
    
    public InvoiceConsumer(
            ScheduledExecutorService scheduledExecutor,
            ExecutorService taskExecutor,
            Cache cacheService,
            StripeInvoiceService stripeInvoiceService) {
        super(scheduledExecutor, taskExecutor, cacheService);
        this.stripeInvoiceService = stripeInvoiceService;
    }
    
    @Override
    public Queue getQueue() {
        return Queue.INVOICE_SYNC;
    }
    
    @Override
    public void processItem(String customerId) {
        try {
            stripeInvoiceService.syncInvoiceData(customerId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing customer " + customerId, e);
            cacheService.queueLeftPush(this.getQueue(), customerId);
        }
    }
}