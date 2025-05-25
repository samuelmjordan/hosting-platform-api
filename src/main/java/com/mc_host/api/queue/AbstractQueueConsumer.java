package com.mc_host.api.queue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mc_host.api.util.Cache;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

public abstract class AbstractQueueConsumer implements QueueConsumer {
    private static final Logger LOGGER = Logger.getLogger(AbstractQueueConsumer.class.getName());

    protected static final long INITIAL_DELAY_MS = 100;
    protected static final long MAX_DELAY_MS = 10000;
    
    private static final RateLimiter GLOBAL_RATE_LIMITER = new RateLimiter(1000);
    
    private final AtomicInteger consecutiveBackoffs = new AtomicInteger(0);
    private final AtomicLong currentDelayMs = new AtomicLong(INITIAL_DELAY_MS);
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    protected final ScheduledExecutorService scheduledExecutor;
    protected final ExecutorService taskExecutor;
    protected final Cache cacheService;
    
    protected AbstractQueueConsumer(
            ScheduledExecutorService scheduledExecutor,
            ExecutorService taskExecutor,
            Cache cacheService) {
        this.scheduledExecutor = scheduledExecutor;
        this.taskExecutor = taskExecutor;
        this.cacheService = cacheService;
    }
    
    @Override
    @PostConstruct
    public void start() {
        if (running.compareAndSet(false, true)) {
            LOGGER.info("Starting consumer for queue: " + getQueue().name());
            scheduleNextPoll();
        }
    }
    
    @Override
    @PreDestroy
    public void stop() {
        if (running.compareAndSet(true, false)) {
            LOGGER.info("Stopping consumer for queue: " + getQueue().name());
        }
    }

    @Override
    public void requeueItem(String item) {
        if (item != null) {
            LOGGER.warning("Requeuing item: " + item + " for queue: " + getQueue().name());
            cacheService.queueLeftPush(getQueue(), item);
        }
    }

    protected void resetBackoff() {
        consecutiveBackoffs.set(0);
        currentDelayMs.set(INITIAL_DELAY_MS);
    }
    
    protected void applyBackoff() {
        int backoffCount = consecutiveBackoffs.incrementAndGet();
        if (backoffCount > 1) {
            long newDelay = Math.min(currentDelayMs.get() * 2, MAX_DELAY_MS);
            currentDelayMs.set(newDelay);
        }
    }
    
    private void scheduleNextPoll() {
        if (running.get()) {
            scheduledExecutor.schedule(this::poll, currentDelayMs.get(), TimeUnit.MILLISECONDS);
        }
    }
    
    private void poll() {
        String item = null;
        try {
            if (!GLOBAL_RATE_LIMITER.tryAcquire()) {
                LOGGER.fine("Rate limited, delaying poll for queue: " + getQueue().name());
                currentDelayMs.set(Math.min(currentDelayMs.get() * 2, MAX_DELAY_MS));
                return;
            }
            
            item = cacheService.queueRead(getQueue());
            if (item == null) {
                applyBackoff();
                return;
            }

            final String finalItem = item;
            taskExecutor.submit(() -> processItem(finalItem));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error polling queue: " + getQueue().name(), e);
            applyBackoff();
        } finally {
            scheduleNextPoll();
        }
    }

}
