package com.rohith.javaprojects.airbnb.dto;

public record StripeCheckoutResult(
        String sessionId,
        String checkoutUrl) {
}
