package com.rohith.javaprojects.airbnb.dto;

import java.util.Map;

public record StripeSessionSnapshot(
        String sessionId,
        String checkoutUrl,
        String status,
        String paymentStatus,
        Long amountTotal,
        String currency,
        String clientReferenceId,
        Map<String, String> metadata,
        String paymentIntentId,
        Boolean liveMode
) {
}