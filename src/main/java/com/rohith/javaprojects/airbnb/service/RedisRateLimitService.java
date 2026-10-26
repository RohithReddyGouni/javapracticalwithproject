package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.dto.RateLimitDecision;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RedisRateLimitService {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<List> script;
    private final CircuitBreaker circuitBreaker;

    public RedisRateLimitService(
            @Qualifier("rateLimitRedisTemplate")
            StringRedisTemplate redisTemplate,

            @Qualifier("rateLimitScript")
            RedisScript<List> script,
            @Qualifier("rateLimitCircuitBreaker")
            CircuitBreaker circuitBreaker) {

        this.redisTemplate = redisTemplate;
        this.script = script;
        this.circuitBreaker = circuitBreaker;
    }

    public RateLimitDecision check(
            String key,
            int limit,
            long windowMillis) {

        if (key == null
                || !key.startsWith("airbnb:ratelimit:v1:")
                || limit < 1
                || windowMillis < 1) {

            throw new IllegalArgumentException(
                    "Invalid rate-limit configuration");
        }

        List<?> result = circuitBreaker.executeSupplier(
                () -> redisTemplate.execute(
                        script,
                        List.of(key),
                        Integer.toString(limit),
                        Long.toString(windowMillis)));

        if (result == null || result.size() != 3) {
            throw new IllegalStateException(
                    "Unexpected rate-limit script result");
        }

        long allowed = ((Number) result.get(0)).longValue();

        long remainingRequests =
                ((Number) result.get(1)).longValue();

        long remainingWindowMillis =
                ((Number) result.get(2)).longValue();

        return new RateLimitDecision(
                allowed == 1,
                remainingRequests,
                remainingWindowMillis);
    }
}