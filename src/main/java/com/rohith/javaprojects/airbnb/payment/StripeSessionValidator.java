package com.rohith.javaprojects.airbnb.payment;

import com.rohith.javaprojects.airbnb.dto.StripeSessionSnapshot;
import com.rohith.javaprojects.airbnb.entity.Booking;
import com.rohith.javaprojects.airbnb.entity.PaymentAttempt;
import com.rohith.javaprojects.airbnb.exception.InvalidWebhookEventException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class StripeSessionValidator {

    public void validate(
            PaymentAttempt attempt,
            Booking booking,
            StripeSessionSnapshot session) {

        if (session.sessionId() == null
                || session.sessionId().isBlank()) {
            reject("Stripe session ID is missing");
        }

        if (!Boolean.FALSE.equals(session.liveMode())) {
            reject("Expected a Stripe test-mode session");
        }

        String storedSessionId = attempt.getStripeSessionId();

        if (storedSessionId != null
                && !storedSessionId.equals(session.sessionId())) {
            reject("Stripe session does not match the payment attempt");
        }

        String bookingId = booking.getId().toString();

        if (!bookingId.equals(session.clientReferenceId())) {
            reject("Stripe booking reference does not match");
        }

        Map<String, String> metadata = session.metadata();

        if (metadata == null
                || !bookingId.equals(metadata.get("bookingId"))
                || !attempt.getAttemptKey().toString()
                .equals(metadata.get("attemptKey"))) {
            reject("Stripe metadata does not match the payment attempt");
        }

        if (!attempt.getExpectedAmountMinor()
                .equals(session.amountTotal())) {
            reject("Stripe amount does not match the expected amount");
        }

        if (session.currency() == null
                || !attempt.getCurrency()
                .equalsIgnoreCase(session.currency())
                || !attempt.getCurrency()
                .equalsIgnoreCase(booking.getCurrency())) {
            reject("Payment currency does not match");
        }

        long bookingAmount = booking.getTotalPrice()
                .movePointRight(2)
                .longValueExact();

        if (attempt.getExpectedAmountMinor() != bookingAmount) {
            reject("Payment attempt amount does not match the booking");
        }

        String storedIntentId = attempt.getStripePaymentIntentId();

        if (storedIntentId != null
                && !storedIntentId.equals(session.paymentIntentId())) {
            reject("Stripe payment intent does not match");
        }

        if ("paid".equals(session.paymentStatus())
                && (session.paymentIntentId() == null
                || session.paymentIntentId().isBlank())) {
            reject("Paid session is missing its payment intent");
        }
    }

    private void reject(String message) {
        throw new InvalidWebhookEventException(message);
    }
}