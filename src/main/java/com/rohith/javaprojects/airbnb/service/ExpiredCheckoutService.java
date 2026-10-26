package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.dto.StripeSessionSnapshot;
import com.rohith.javaprojects.airbnb.entity.Booking;
import com.rohith.javaprojects.airbnb.entity.PaymentAttempt;
import com.rohith.javaprojects.airbnb.entity.enums.PaymentStatus;
import com.rohith.javaprojects.airbnb.exception.InvalidWebhookEventException;
import com.rohith.javaprojects.airbnb.exception.ResourceConflictException;
import com.rohith.javaprojects.airbnb.payment.StripeSessionValidator;
import com.rohith.javaprojects.airbnb.repository.BookingRepository;
import com.rohith.javaprojects.airbnb.repository.PaymentAttemptRepository;
import com.rohith.javaprojects.airbnb.repository.RoomRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ExpiredCheckoutService {

    private final PaymentAttemptRepository paymentAttemptRepository;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final StripeSessionValidator stripeSessionValidator;

    @PersistenceContext
    private EntityManager entityManager;


    public ExpiredCheckoutService(PaymentAttemptRepository paymentAttemptRepository, BookingRepository bookingRepository, RoomRepository roomRepository, StripeSessionValidator stripeSessionValidator) {
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.stripeSessionValidator = stripeSessionValidator;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordExpiredSession(StripeSessionSnapshot session) {
        String value = session.metadata() == null
                ? null
                : session.metadata().get("attemptKey");

        UUID attemptKey;

        try {
            if (value == null) {
                throw new IllegalArgumentException();
            }

            attemptKey = UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidWebhookEventException(
                    "Payment attempt reference is missing or invalid");
        }

        PaymentAttempt initialAttempt = paymentAttemptRepository
                .findByAttemptKey(attemptKey)
                .orElseThrow(() -> new InvalidWebhookEventException(
                        "Payment attempt is not recognized"));

        Long bookingId = initialAttempt.getBooking().getId();
        Long roomId = initialAttempt.getBooking().getRoom().getId();

        roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new ResourceConflictException(
                        "Booking room is missing"));

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceConflictException(
                        "Booking is missing"));

        entityManager.refresh(booking);

        PaymentAttempt attempt = paymentAttemptRepository
                .findByAttemptKeyForUpdate(attemptKey)
                .orElseThrow(() -> new ResourceConflictException(
                        "Payment attempt is missing"));

        entityManager.refresh(attempt);

        stripeSessionValidator.validate(attempt, booking, session);

        if (!"expired".equals(session.status())
                || !"unpaid".equals(session.paymentStatus())) {
            throw new InvalidWebhookEventException(
                    "Expected an expired unpaid checkout session");
        }

        switch (attempt.getStatus()) {
            case INITIATED, CHECKOUT_CREATED, RECONCILIATION_REQUIRED -> {
                attempt.setStripeSessionId(session.sessionId());
                attempt.setStatus(PaymentStatus.FAILED);
                attempt.setUpdatedAt(Instant.now());
            }

            case FAILED -> {
                // Already recorded: no changes needed.
            }

            default -> throw new InvalidWebhookEventException(
                    "Expired session conflicts with recorded payment state");
        }
    }
}
