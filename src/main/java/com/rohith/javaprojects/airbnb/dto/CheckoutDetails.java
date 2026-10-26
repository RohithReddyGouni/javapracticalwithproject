package com.rohith.javaprojects.airbnb.dto;

import java.util.UUID;

public record CheckoutDetails(
    UUID attemptKey,
    Long bookingId,
    long amountMinor,
    String currency,
    String stripeSessionId) {

}
