package com.mc_host.api.queue;

import java.util.concurrent.atomic.AtomicInteger;

class RateLimiter {
    private final long maxOpsPerSecond;
    private final AtomicInteger currentOps = new AtomicInteger(0);
    private volatile long currentSecond = System.currentTimeMillis() / 1000;
    
    public RateLimiter(long maxOpsPerSecond) {
        this.maxOpsPerSecond = maxOpsPerSecond;
    }
    
    public boolean tryAcquire() {
        long currentSecondNow = System.currentTimeMillis() / 1000;
        if (currentSecondNow > currentSecond) {
            currentSecond = currentSecondNow;
            currentOps.set(0);
        }
        
        int current = currentOps.get();
        if (current >= maxOpsPerSecond) {
            return false;
        }
        
        return currentOps.incrementAndGet() <= maxOpsPerSecond;
    }
}
