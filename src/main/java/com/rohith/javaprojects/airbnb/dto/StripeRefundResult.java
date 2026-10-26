package com.rohith.javaprojects.airbnb.dto;

public record StripeRefundResult(
        String refundId,
        String status,
        Long amountMinor,
        String currency,
        String paymentIntentId,
        String refundKey
) {
}