package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.dto.StripeSessionSnapshot;
import com.rohith.javaprojects.airbnb.entity.Booking;
import com.rohith.javaprojects.airbnb.entity.Inventory;
import com.rohith.javaprojects.airbnb.entity.PaymentAttempt;
import com.rohith.javaprojects.airbnb.entity.enums.BookingStatus;
import com.rohith.javaprojects.airbnb.entity.enums.PaymentStatus;
import com.rohith.javaprojects.airbnb.exception.InvalidWebhookEventException;
import com.rohith.javaprojects.airbnb.exception.ResourceConflictException;
import com.rohith.javaprojects.airbnb.payment.StripeSessionValidator;
import com.rohith.javaprojects.airbnb.repository.BookingRepository;
import com.rohith.javaprojects.airbnb.repository.InventoryRepository;
import com.rohith.javaprojects.airbnb.repository.PaymentAttemptRepository;
import com.rohith.javaprojects.airbnb.repository.RoomRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentConfirmationService {

    private final PaymentAttemptRepository paymentAttemptRepository;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;
    private final StripeSessionValidator stripeSessionValidator;
    private final RefundEnqueueService refundEnqueueService;
    private final OutboxService outboxService;

    @PersistenceContext
    private EntityManager entityManager;


    public PaymentConfirmationService(PaymentAttemptRepository paymentAttemptRepository, BookingRepository bookingRepository, RoomRepository roomRepository, InventoryRepository inventoryRepository, StripeSessionValidator stripeSessionValidator, RefundEnqueueService refundEnqueueService, OutboxService outboxService) {
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.inventoryRepository = inventoryRepository;
        this.stripeSessionValidator = stripeSessionValidator;
        this.refundEnqueueService = refundEnqueueService;
        this.outboxService = outboxService;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void confirmPaidSession(StripeSessionSnapshot session) {

        UUID attemptKey = extractAttemptKey(session);

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

        if (!"complete".equals(session.status())
                || !"paid".equals(session.paymentStatus())) {
            throw new InvalidWebhookEventException(
                    "Checkout session is not a completed paid session");
        }

        if (attempt.getStatus() == PaymentStatus.SUCCEEDED
                || attempt.getStatus() == PaymentStatus.REFUNDED) {
            return;
        }

        if (attempt.getStatus() == PaymentStatus.REFUND_PENDING) {
            refundEnqueueService.enqueueFullRefund(attempt);
            return;
        }

        boolean awaitingPayment =
                attempt.getStatus() == PaymentStatus.INITIATED
                        || attempt.getStatus() == PaymentStatus.CHECKOUT_CREATED
                        || attempt.getStatus()
                        == PaymentStatus.RECONCILIATION_REQUIRED;

        if (!awaitingPayment) {
            throw new ResourceConflictException(
                    "Payment attempt requires separate recovery handling");
        }
        Instant now = Instant.now();

        if (booking.getStatus() != BookingStatus.RESERVED
                || !booking.getReservedUntil().isAfter(now)) {

            markRefundRequired(attempt, booking, session, now);
            return;
        }

        List<Inventory> rows = inventoryRepository
                .findStayInventoryForUpdate(
                        roomId,
                        booking.getCheckInDate(),
                        booking.getCheckOutDate());

        long nights = ChronoUnit.DAYS.between(
                booking.getCheckInDate(),
                booking.getCheckOutDate());

        if (rows.size() != nights) {
            throw new ResourceConflictException(
                    "Inventory is incomplete for payment confirmation");
        }

        for (Inventory inventory : rows) {
            if (inventory.getReservedCount() < booking.getRoomsCount()) {
                throw new ResourceConflictException(
                        "Reserved inventory is inconsistent");
            }
        }

        for (Inventory inventory : rows) {
            inventory.setReservedCount(
                    inventory.getReservedCount() - booking.getRoomsCount());

            inventory.setBookedCount(
                    inventory.getBookedCount() + booking.getRoomsCount());
        }

        attempt.setStripeSessionId(session.sessionId());
        attempt.setStripePaymentIntentId(session.paymentIntentId());
        attempt.setStatus(PaymentStatus.SUCCEEDED);
        attempt.setUpdatedAt(Instant.now());

        booking.setStatus(BookingStatus.CONFIRMED);
        outboxService.recordBookingConfirmed(booking);
    }

    private void markRefundRequired(
            PaymentAttempt attempt,
            Booking booking,
            StripeSessionSnapshot session,
            Instant now) {

        boolean expiredHoldStillRecorded =
                booking.getStatus() == BookingStatus.RESERVED
                        && !booking.getReservedUntil().isAfter(now);

        if (expiredHoldStillRecorded) {
            List<Inventory> rows = inventoryRepository
                    .findStayInventoryForUpdate(
                            booking.getRoom().getId(),
                            booking.getCheckInDate(),
                            booking.getCheckOutDate());

            long nights = ChronoUnit.DAYS.between(
                    booking.getCheckInDate(),
                    booking.getCheckOutDate());

            if (rows.size() != nights) {
                throw new ResourceConflictException(
                        "Inventory is incomplete for late-payment recovery");
            }

            for (Inventory inventory : rows) {
                if (inventory.getReservedCount() < booking.getRoomsCount()) {
                    throw new ResourceConflictException(
                            "Reserved inventory is inconsistent");
                }
            }

            for (Inventory inventory : rows) {
                inventory.setReservedCount(
                        inventory.getReservedCount()
                                - booking.getRoomsCount());
            }

            booking.setStatus(BookingStatus.EXPIRED);
        }

        attempt.setStripeSessionId(session.sessionId());
        attempt.setStripePaymentIntentId(session.paymentIntentId());
        attempt.setStatus(PaymentStatus.REFUND_PENDING);
        attempt.setUpdatedAt(now);

        refundEnqueueService.enqueueFullRefund(attempt);
    }


    private UUID extractAttemptKey(StripeSessionSnapshot session) {
        String value = session.metadata() == null
                ? null
                : session.metadata().get("attemptKey");

        if (value == null) {
            throw new InvalidWebhookEventException(
                    "Payment attempt reference is missing");
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidWebhookEventException(
                    "Payment attempt reference is invalid");
        }
    }

}
