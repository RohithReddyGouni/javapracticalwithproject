package com.rohith.javaprojects.airbnb.dto;

import java.util.UUID;

public record WebhookClaim(
        String eventId,
        UUID leaseToken,
        String eventType,
        String payload) {
}
