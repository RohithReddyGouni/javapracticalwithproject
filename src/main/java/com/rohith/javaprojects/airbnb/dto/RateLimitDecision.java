package com.rohith.javaprojects.airbnb.dto;

public record RateLimitDecision(
        boolean allowed,
        long remainingRequests,
        long remainingWindowMillis
) {
}