package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.dto.StripeSessionSnapshot;
import com.rohith.javaprojects.airbnb.dto.WebhookClaim;
import com.rohith.javaprojects.airbnb.entity.StripeWebhookEvent;
import com.rohith.javaprojects.airbnb.entity.enums.WebhookStatus;
import com.rohith.javaprojects.airbnb.exception.InvalidWebhookEventException;
import com.rohith.javaprojects.airbnb.exception.LostWebhookLeaseException;
import com.rohith.javaprojects.airbnb.repository.StripeWebhookEventRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class WebhookPaymentProcessor {

    private final StripeWebhookEventRepository repository;
    private final PaymentConfirmationService confirmationService;
    private final WebhookProgressService progressService;
    private final ExpiredCheckoutService expiredCheckoutService;

    @PersistenceContext
    private EntityManager entityManager;

    public WebhookPaymentProcessor(
            StripeWebhookEventRepository repository,
            PaymentConfirmationService confirmationService,
            WebhookProgressService progressService,
            ExpiredCheckoutService expiredCheckoutService) {

        this.repository = repository;
        this.confirmationService = confirmationService;
        this.progressService = progressService;
        this.expiredCheckoutService = expiredCheckoutService;
    }

    @Transactional
    public void processCheckoutEvent(
            WebhookClaim claim,
            StripeSessionSnapshot session) {

        StripeWebhookEvent event = repository
                .findByEventIdForUpdate(claim.eventId())
                .orElseThrow(LostWebhookLeaseException::new);

        entityManager.refresh(event);

        boolean ownsLease =
                event.getStatus() == WebhookStatus.PROCESSING
                && claim.leaseToken().equals(event.getLeaseToken())
                && event.getLeaseUntil() != null
                && event.getLeaseUntil().isAfter(Instant.now());

        if (!ownsLease) {
            throw new LostWebhookLeaseException();
        }

        if (!event.getEventType().equals(claim.eventType())
                || !event.getPayload().equals(claim.payload())) {
            throw new InvalidWebhookEventException(
                    "Worker claim does not match the stored event");
        }

        boolean supportedEvent =
                "checkout.session.completed".equals(event.getEventType())
                        || "checkout.session.expired".equals(event.getEventType());

        if (!supportedEvent) {
            throw new InvalidWebhookEventException(
                    "Unsupported checkout event type");
        }

        boolean completedAndPaid =
                "complete".equals(session.status())
                        && "paid".equals(session.paymentStatus());

        boolean expiredAndUnpaid =
                "expired".equals(session.status())
                        && "unpaid".equals(session.paymentStatus());

        if (completedAndPaid) {
            confirmationService.confirmPaidSession(session);
        } else if (expiredAndUnpaid) {
            expiredCheckoutService.recordExpiredSession(session);
        } else {
            throw new InvalidWebhookEventException(
                    "Stripe session is not in a supported terminal state");
        }

        if (!progressService.complete(claim)) {
            throw new LostWebhookLeaseException();
        }
    }
}