package com.mc_host.api.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class AsyncConfiguration {

    @Bean()
    public ScheduledExecutorService delayedTaskScheduler() {
        return Executors.newScheduledThreadPool(10); // roughly num of queues * 2
    }
}