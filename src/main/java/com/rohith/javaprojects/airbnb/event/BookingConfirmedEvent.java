package com.rohith.javaprojects.airbnb.event;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BookingConfirmedEvent(
        UUID eventId,
        String eventType,
        int schemaVersion,
        @JsonAlias("occuredAt")
        Instant occurredAt,
        Long bookingId,
        Long userId,
        Long roomId,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Integer roomsCount,
        BigDecimal totalPrice,
        String currency) {
}
