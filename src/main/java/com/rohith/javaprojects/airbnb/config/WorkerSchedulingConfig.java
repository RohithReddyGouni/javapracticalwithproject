package com.rohith.javaprojects.airbnb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class WorkerSchedulingConfig {

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        return createScheduler("scheduled-default-");
    }

    @Bean
    public ThreadPoolTaskScheduler bookingExpiryScheduler() {
        return createScheduler("booking-expiry-");
    }

    @Bean
    public ThreadPoolTaskScheduler webhookScheduler() {
        return createScheduler("stripe-webhook-");
    }

    @Bean
    public ThreadPoolTaskScheduler refundScheduler() {
        return createScheduler("stripe-refund-");
    }

    @Bean
    public ThreadPoolTaskScheduler outboxScheduler() {
        return createScheduler("outbox-publisher-");
    }

    private ThreadPoolTaskScheduler createScheduler(
            String threadNamePrefix) {

        ThreadPoolTaskScheduler scheduler =
                new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix(threadNamePrefix);
        scheduler.setRemoveOnCancelPolicy(true);

        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(45);

        return scheduler;
    }
}