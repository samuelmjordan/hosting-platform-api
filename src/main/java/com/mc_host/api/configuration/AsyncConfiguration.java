package com.mc_host.api.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfiguration {

    @Bean
    public ScheduledExecutorService delayedTaskScheduler() {
        return Executors.newScheduledThreadPool(10); // roughly num of queues * 2
    }

    @Bean
    @Primary
    public ThreadPoolExecutor threadPoolExecutor() {
        return (ThreadPoolExecutor) Executors.newFixedThreadPool(40);
    }

}