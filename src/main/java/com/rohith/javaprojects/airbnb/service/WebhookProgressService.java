package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.dto.WebhookClaim;
import com.rohith.javaprojects.airbnb.entity.StripeWebhookEvent;
import com.rohith.javaprojects.airbnb.entity.enums.WebhookFailureCode;
import com.rohith.javaprojects.airbnb.entity.enums.WebhookStatus;
import com.rohith.javaprojects.airbnb.repository.StripeWebhookEventRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class WebhookProgressService {

    private final StripeWebhookEventRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    public WebhookProgressService(
            StripeWebhookEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean complete(WebhookClaim claim) {
        StripeWebhookEvent event = loadCurrentClaim(claim);

        if (event == null) {
            return false;
        }

        event.setStatus(WebhookStatus.PROCESSED);
        event.setProcessedAt(Instant.now());
        event.setLeaseToken(null);
        event.setLeaseUntil(null);
        event.setLastErrorCode(null);

        return true;
    }

    @Transactional
    public boolean scheduleRetry(WebhookClaim claim) {
        StripeWebhookEvent event = loadCurrentClaim(claim);

        if (event == null) {
            return false;
        }

        event.setLastErrorCode("TRANSIENT_PROCESSING_FAILURE");
        event.setLeaseToken(null);
        event.setLeaseUntil(null);

        if (event.getAttempts() >= 5) {
            event.setStatus(WebhookStatus.FAILED);
            return true;
        }

        long delaySeconds =
                5L << Math.min(event.getAttempts() - 1, 6);

        event.setStatus(WebhookStatus.PENDING);
        event.setNextAttemptAt(
                Instant.now().plusSeconds(delaySeconds));

        return true;
    }

    private StripeWebhookEvent loadCurrentClaim(WebhookClaim claim) {
        StripeWebhookEvent event = repository
                .findByEventIdForUpdate(claim.eventId())
                .orElse(null);

        if (event == null) {
            return null;
        }

        entityManager.refresh(event);

        boolean ownsLease =
                event.getStatus() == WebhookStatus.PROCESSING
                        && claim.leaseToken().equals(event.getLeaseToken())
                        && event.getLeaseUntil() != null
                        && event.getLeaseUntil().isAfter(Instant.now());

        return ownsLease ? event : null;
    }

    @Transactional
    public boolean failPermanently(
            WebhookClaim claim,
            WebhookFailureCode failureCode) {

        StripeWebhookEvent event = loadCurrentClaim(claim);

        if (event == null) {
            return false;
        }

        event.setStatus(WebhookStatus.FAILED);
        event.setLastErrorCode(failureCode.name());
        event.setLeaseToken(null);
        event.setLeaseUntil(null);

        return true;
    }
}
