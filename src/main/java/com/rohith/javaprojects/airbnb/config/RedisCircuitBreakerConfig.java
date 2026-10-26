package com.rohith.javaprojects.airbnb.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;

import java.time.Duration;
import java.util.Map;

@Configuration
public class RedisCircuitBreakerConfig {

    private static final Logger log =
            LoggerFactory.getLogger(RedisCircuitBreakerConfig.class);

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(
            MeterRegistry meterRegistry) {

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(
                        CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(2)
                .automaticTransitionFromOpenToHalfOpenEnabled(false)
                .ignoreException(
                        exception ->
                                !(exception instanceof DataAccessException))
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(
                Map.of("redisRateLimit", config));

        TaggedCircuitBreakerMetrics
                .ofCircuitBreakerRegistry(registry)
                .bindTo(meterRegistry);

        return registry;
    }

    @Bean
    public CircuitBreaker rateLimitCircuitBreaker(
            CircuitBreakerRegistry registry) {

        CircuitBreaker breaker = registry.circuitBreaker(
                "hotelSearchRateLimit",
                "redisRateLimit");

        breaker.getEventPublisher()
                .onStateTransition(event ->
                        log.info(
                                "Circuit breaker {} transition={}",
                                event.getCircuitBreakerName(),
                                event.getStateTransition()));

        return breaker;
    }
}