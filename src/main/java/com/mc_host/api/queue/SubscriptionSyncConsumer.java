package com.mc_host.api.queue;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.configuration.StripeConfiguration;
import com.mc_host.api.model.cache.CacheNamespace;
import com.mc_host.api.model.cache.Queue;
import com.mc_host.api.service.stripe.StripeSubscriptionService;
import com.mc_host.api.util.CacheService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class SubscriptionSyncConsumer {
    private static final Logger LOGGER = Logger.getLogger(SubscriptionSyncConsumer.class.getName());

    private static final long INITIAL_DELAY_MS = 100;
    private static final long MAX_DELAY_MS = 5000;
    
    private final AtomicInteger consecutiveEmptyPolls = new AtomicInteger(0);
    private final AtomicLong currentDelayMs = new AtomicLong(INITIAL_DELAY_MS);
    private final AtomicBoolean running = new AtomicBoolean(true);
    
    private final StripeConfiguration stripeConfiguration;
    private final ScheduledExecutorService scheduledExecutor;
    private final ExecutorService taskExecutor;
    private final CacheService cacheService;
    private final StripeSubscriptionService stripeSubscriptionService;
    
    public SubscriptionSyncConsumer(
            StripeConfiguration stripeConfiguration,
            ScheduledExecutorService scheduledExecutor,
            ExecutorService taskExecutor,
            CacheService cacheService,
            StripeSubscriptionService stripeSubscriptionService) {
        this.stripeConfiguration = stripeConfiguration;
        this.scheduledExecutor = scheduledExecutor;
        this.taskExecutor = taskExecutor;
        this.cacheService = cacheService;
        this.stripeSubscriptionService = stripeSubscriptionService;
    }
    
    @PostConstruct
    public void init() {
        scheduleNextPoll();
    }
    
    @PreDestroy
    public void shutdown() {
        running.set(false);
        LOGGER.info("Subscription sync consumer shutting down");
    }
    
    private void scheduleNextPoll() {
        if (running.get()) {
            scheduledExecutor.schedule(this::poll, currentDelayMs.get(), TimeUnit.MILLISECONDS);
        }
    }
    
    private void poll() {
        try {
            String customerId = cacheService.queueRead(Queue.SUBSCRIPTION_SYNC);
            if (customerId != null) {
                resetBackoff();
                final String finalCustomerId = customerId;
                if (cacheService.flagIfAbsent(
                        CacheNamespace.SUBSCRIPTION_SYNC_IN_PROGRESS, 
                        finalCustomerId,    
                        Duration.ofMinutes(stripeConfiguration.getSubscriptionSyncTimeoutMinutes()))) {
                    taskExecutor.submit(() -> processCustomer(finalCustomerId));
                } else {
                    cacheService.queuePush(Queue.SUBSCRIPTION_SYNC, finalCustomerId);
                }
            } else {
                applyBackoff();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error polling queue", e);
            applyBackoff();
        } finally {
            scheduleNextPoll();
        }
    }
    
    private void processCustomer(String customerId) {
        try {
            stripeSubscriptionService.handleCustomerSubscriptionSync(customerId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing customer " + customerId, e);
        } finally {
            cacheService.evict(CacheNamespace.SUBSCRIPTION_SYNC_IN_PROGRESS, customerId);
        }
    }
    
    private void resetBackoff() {
        consecutiveEmptyPolls.set(0);
        currentDelayMs.set(INITIAL_DELAY_MS);
    }
    
    private void applyBackoff() {
        int emptyCount = consecutiveEmptyPolls.incrementAndGet();
        if (emptyCount > 1) {
            long newDelay = Math.min(currentDelayMs.get() * 2, MAX_DELAY_MS);
            currentDelayMs.set(newDelay);
        }
    }
}