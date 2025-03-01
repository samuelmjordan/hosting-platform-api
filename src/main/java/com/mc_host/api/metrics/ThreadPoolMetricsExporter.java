package com.mc_host.api.metrics;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;

@Component
public class ThreadPoolMetricsExporter {
    
    private final ScheduledThreadPoolExecutor threadPool;
    private final MeterRegistry meterRegistry;
    
    public ThreadPoolMetricsExporter(ScheduledExecutorService delayedTaskScheduler, MeterRegistry meterRegistry) {
        this.threadPool = (ScheduledThreadPoolExecutor) delayedTaskScheduler;
        this.meterRegistry = meterRegistry;
    }
    
    @PostConstruct
    public void registerMetrics() {
        Gauge.builder("thread.pool.active.count", threadPool, ScheduledThreadPoolExecutor::getActiveCount)
                .description("The approximate number of threads executing tasks")
                .tag("pool", "delayedTaskScheduler")
                .register(meterRegistry);
                
        Gauge.builder("thread.pool.size", threadPool, ScheduledThreadPoolExecutor::getPoolSize)
                .description("The current number of threads in the pool")
                .tag("pool", "delayedTaskScheduler")
                .register(meterRegistry);
                
        Gauge.builder("thread.pool.queue.size", threadPool, tp -> tp.getQueue().size())
                .description("The number of tasks waiting in the queue")
                .tag("pool", "delayedTaskScheduler")
                .register(meterRegistry);
                
        Gauge.builder("thread.pool.available.threads", threadPool, 
                      tp -> tp.getCorePoolSize() - tp.getActiveCount())
                .description("The number of available threads")
                .tag("pool", "delayedTaskScheduler")
                .register(meterRegistry);
                
        Gauge.builder("thread.pool.completed.tasks", threadPool, ScheduledThreadPoolExecutor::getCompletedTaskCount)
                .description("The number of completed tasks")
                .tag("pool", "delayedTaskScheduler")
                .register(meterRegistry);
    }
}
