package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.dto.CheckoutDetails;
import com.rohith.javaprojects.airbnb.dto.StripeCheckoutResult;
import com.rohith.javaprojects.airbnb.dto.StripeSessionSnapshot;
import com.rohith.javaprojects.airbnb.entity.Booking;
import com.rohith.javaprojects.airbnb.entity.PaymentAttempt;
import com.rohith.javaprojects.airbnb.entity.enums.BookingStatus;
import com.rohith.javaprojects.airbnb.entity.enums.PaymentStatus;
import com.rohith.javaprojects.airbnb.exception.ResourceConflictException;
import com.rohith.javaprojects.airbnb.exception.ResourceNotFoundException;
import com.rohith.javaprojects.airbnb.repository.BookingRepository;
import com.rohith.javaprojects.airbnb.repository.PaymentAttemptRepository;
import com.rohith.javaprojects.airbnb.repository.RoomRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentAttemptService {

    private final BookingRepository bookingRepository;

    private final RoomRepository roomRepository;

    private final PaymentAttemptRepository paymentAttemptRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public PaymentAttemptService(BookingRepository bookingRepository, RoomRepository roomRepository, PaymentAttemptRepository paymentAttemptRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
    }

   @Transactional
    public UUID prepareAttempt(Long userId, Long bookingId) {
        Booking initialBooking = bookingRepository
                .findByIdAndUser_Id(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with ID: " + bookingId));

        Long roomId = initialBooking.getRoom().getId();

        roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Room not found with ID: " + roomId));

        Booking booking = bookingRepository
                .findOwnedBookingForUpdate(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with ID: " + bookingId));

        entityManager.refresh(booking);

        if (booking.getStatus() != BookingStatus.RESERVED
                || !booking.getReservedUntil().isAfter(Instant.now())) {
            throw new ResourceConflictException(
                    "Booking is not an active unpaid reservation");
        }

        PaymentAttempt existing = paymentAttemptRepository
                .findFirstByBooking_IdOrderByIdDesc(bookingId)
                .orElse(null);

        if (existing != null) {
            switch (existing.getStatus()) {
                case INITIATED, CHECKOUT_CREATED,
                     RECONCILIATION_REQUIRED -> {
                    return existing.getAttemptKey();
                }
                case FAILED -> {
                    // A definitively failed attempt permits a new attempt.
                }
                default -> throw new ResourceConflictException(
                        "Payment requires review before another attempt");
            }
        }

        Instant now = Instant.now();

        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setAttemptKey(UUID.randomUUID());
        attempt.setBooking(booking);
        attempt.setExpectedAmountMinor(
                booking.getTotalPrice()
                        .movePointRight(2)
                        .longValueExact());
        attempt.setCurrency(booking.getCurrency());
        attempt.setStatus(PaymentStatus.INITIATED);
        attempt.setCreatedAt(now);
        attempt.setUpdatedAt(now);

        PaymentAttempt saved = paymentAttemptRepository.save(attempt);
        return saved.getAttemptKey();
    }

    @Transactional(readOnly = true)
    public CheckoutDetails loadCheckoutDetails(
            Long userId, UUID attemptKey) {
        PaymentAttempt attempt = paymentAttemptRepository
                .findByAttemptKeyAndBooking_User_Id(attemptKey, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment attempt not found"));

        Booking booking = attempt.getBooking();

        if (booking.getStatus() != BookingStatus.RESERVED
                || !booking.getReservedUntil().isAfter(Instant.now())) {
            throw new ResourceConflictException(
                    "Booking is not an active unpaid reservation");
        }

        if (attempt.getStatus() != PaymentStatus.INITIATED
                && attempt.getStatus() != PaymentStatus.CHECKOUT_CREATED
                && attempt.getStatus()
                != PaymentStatus.RECONCILIATION_REQUIRED) {
            throw new ResourceConflictException(
                    "Payment attempt cannot proceed to checkout");
        }

        return new CheckoutDetails(
                attempt.getAttemptKey(),
                booking.getId(),
                attempt.getExpectedAmountMinor(),
                attempt.getCurrency(),
                attempt.getStripeSessionId());
    }

    @Transactional
    public boolean recordCheckoutSession(
            Long userId,
            UUID attemptKey,
            StripeSessionSnapshot result) {

        PaymentAttempt initialAttempt = paymentAttemptRepository
                .findByAttemptKeyAndBooking_User_Id(attemptKey, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment attempt not found"));

        Long bookingId = initialAttempt.getBooking().getId();
        Long roomId = initialAttempt.getBooking().getRoom().getId();

        roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Room not found"));

        Booking booking = bookingRepository
                .findOwnedBookingForUpdate(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found"));

        entityManager.refresh(booking);

        PaymentAttempt attempt = paymentAttemptRepository
                .findOwnedAttemptForUpdate(attemptKey, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment attempt not found"));

        entityManager.refresh(attempt);

        String existingSessionId = attempt.getStripeSessionId();

        if (existingSessionId != null
                && !existingSessionId.equals(result.sessionId())) {
            throw new ResourceConflictException(
                    "Payment attempt already references a different Stripe session");
        }

        attempt.setStripeSessionId(result.sessionId());
        attempt.setUpdatedAt(Instant.now());

        boolean awaitingCheckout =
                attempt.getStatus() == PaymentStatus.INITIATED
                        || attempt.getStatus() == PaymentStatus.CHECKOUT_CREATED
                        || attempt.getStatus()
                        == PaymentStatus.RECONCILIATION_REQUIRED;

        if (!awaitingCheckout) {
            // Preserve succeeded, failed, or refund-related states.
            return false;
        }

        boolean activeReservation =
                booking.getStatus() == BookingStatus.RESERVED
                        && booking.getReservedUntil().isAfter(Instant.now());
        boolean usableSession =
                "open".equals(result.status())
                        && "unpaid".equals(result.paymentStatus())
                        && result.checkoutUrl() != null
                        && !result.checkoutUrl().isBlank();


        if (!activeReservation || !usableSession) {
            attempt.setStatus(PaymentStatus.RECONCILIATION_REQUIRED);
            return false;
        }

        attempt.setStatus(PaymentStatus.CHECKOUT_CREATED);
        return true;
    }
}
