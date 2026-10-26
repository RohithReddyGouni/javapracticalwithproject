package com.rohith.javaprojects.airbnb.dto;

import java.util.UUID;

public record RefundDetails(
        UUID refundKey,
        String paymentIntentId,
        long amountMinor,
        String stripeRefundId
) {
}