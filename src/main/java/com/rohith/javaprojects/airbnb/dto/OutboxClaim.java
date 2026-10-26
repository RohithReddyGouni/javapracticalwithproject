package com.rohith.javaprojects.airbnb.dto;

import java.util.UUID;

public record OutboxClaim(
        UUID eventId,
        UUID leaseToken,
        String messageKey,
        String payload) {
}