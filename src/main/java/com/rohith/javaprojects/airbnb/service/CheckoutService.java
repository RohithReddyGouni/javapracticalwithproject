package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.dto.CheckoutDetails;
import com.rohith.javaprojects.airbnb.dto.StripeCheckoutResult;
import com.rohith.javaprojects.airbnb.dto.StripeSessionSnapshot;
import com.rohith.javaprojects.airbnb.exception.ResourceConflictException;
import com.rohith.javaprojects.airbnb.payment.StripeCheckoutGateway;
import com.stripe.exception.StripeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CheckoutService {

    private final PaymentAttemptService paymentAttemptService;
    private final StripeCheckoutGateway stripeCheckoutGateway;

    public CheckoutService(
            PaymentAttemptService paymentAttemptService,
            StripeCheckoutGateway stripeCheckoutGateway) {

        this.paymentAttemptService = paymentAttemptService;
        this.stripeCheckoutGateway = stripeCheckoutGateway;
    }

    @Transactional(propagation = Propagation.NEVER)
    public StripeCheckoutResult checkout(
            Long userId, Long bookingId) throws StripeException {

        UUID attemptKey =
                paymentAttemptService.prepareAttempt(userId, bookingId);

        CheckoutDetails details =
                paymentAttemptService.loadCheckoutDetails(
                        userId, attemptKey);

        String sessionId = details.stripeSessionId();

        if (sessionId == null) {
            StripeCheckoutResult created =
                    stripeCheckoutGateway.createCheckout(details);

            sessionId = created.sessionId();
        }

        StripeSessionSnapshot snapshot =
                stripeCheckoutGateway.retrieveSession(sessionId);

        boolean usable =
                paymentAttemptService.recordCheckoutSession(
                        userId, attemptKey, snapshot);

        if (!usable) {
            stripeCheckoutGateway.expireIfOpen(sessionId);

            throw new ResourceConflictException(
                    "Checkout is unavailable; payment status requires reconciliation");
        }

        return new StripeCheckoutResult(
                snapshot.sessionId(),
                snapshot.checkoutUrl());
    }
}