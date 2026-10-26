package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.event.BookingConfirmedEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class BookingConfirmationConsumerService {

    private static final String CONSUMER_NAME =
            "booking-confirmation-report-v1";

    private static final BigDecimal MAX_TOTAL_PRICE =
            new BigDecimal("999999999999.99");

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public boolean process(BookingConfirmedEvent event) {

        validate(event);

        int inserted = entityManager.createNativeQuery("""
                INSERT INTO consumer_inbox (
                    consumer_name,
                    event_id,
                    event_type
                )
                VALUES (
                    :consumerName,
                    :eventId,
                    :eventType
                )
                ON CONFLICT (consumer_name, event_id)
                DO NOTHING
                """)
                .setParameter("consumerName", CONSUMER_NAME)
                .setParameter("eventId", event.eventId())
                .setParameter("eventType", event.eventType())
                .executeUpdate();

        if (inserted == 0) {
            return false;
        }

        entityManager.createNativeQuery("""
                INSERT INTO booking_confirmation_summaries (
                    booking_id,
                    event_id,
                    user_id,
                    room_id,
                    check_in_date,
                    check_out_date,
                    rooms_count,
                    total_price,
                    currency,
                    confirmed_at
                )
                VALUES (
                    :bookingId,
                    :eventId,
                    :userId,
                    :roomId,
                    :checkInDate,
                    :checkOutDate,
                    :roomsCount,
                    :totalPrice,
                    :currency,
                    :confirmedAt
                )
                """)
                .setParameter("bookingId", event.bookingId())
                .setParameter("eventId", event.eventId())
                .setParameter("userId", event.userId())
                .setParameter("roomId", event.roomId())
                .setParameter("checkInDate", event.checkInDate())
                .setParameter("checkOutDate", event.checkOutDate())
                .setParameter("roomsCount", event.roomsCount())
                .setParameter("totalPrice", event.totalPrice())
                .setParameter("currency", event.currency())
                .setParameter("confirmedAt", event.occurredAt())
                .executeUpdate();

        return true;
    }

    private void validate(BookingConfirmedEvent event) {

        if (event == null
                || event.eventId() == null
                || event.occurredAt() == null) {
            throw new IllegalArgumentException(
                    "Event ID and occurrence time are required");
        }

        if (!"BookingConfirmed".equals(event.eventType())
                || event.schemaVersion() != 1) {
            throw new IllegalArgumentException(
                    "Unsupported event type or schema version");
        }

        if (!isPositive(event.bookingId())
                || !isPositive(event.userId())
                || !isPositive(event.roomId())) {
            throw new IllegalArgumentException(
                    "Booking, user and room IDs must be positive");
        }

        if (event.checkInDate() == null
                || event.checkOutDate() == null
                || !event.checkOutDate()
                        .isAfter(event.checkInDate())) {
            throw new IllegalArgumentException(
                    "Invalid booking date range");
        }

        if (event.roomsCount() == null
                || event.roomsCount() < 1) {
            throw new IllegalArgumentException(
                    "Room count must be positive");
        }

        BigDecimal price = event.totalPrice();

        if (price == null
                || price.signum() <= 0
                || price.compareTo(MAX_TOTAL_PRICE) > 0
                || price.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(
                    "Invalid total price");
        }

        if (!"USD".equals(event.currency())) {
            throw new IllegalArgumentException(
                    "Unsupported currency");
        }
    }

    private boolean isPositive(Long value) {
        return value != null && value > 0;
    }
}