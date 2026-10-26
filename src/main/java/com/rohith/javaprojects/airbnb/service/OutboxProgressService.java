package com.rohith.javaprojects.airbnb.service;


import com.rohith.javaprojects.airbnb.dto.OutboxClaim;
import com.rohith.javaprojects.airbnb.entity.OutboxEvent;
import com.rohith.javaprojects.airbnb.entity.enums.OutboxStatus;
import com.rohith.javaprojects.airbnb.repository.OutboxEventRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class OutboxProgressService {

    private static final int MAX_ATTEMPTS = 10;

    private final OutboxEventRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    public OutboxProgressService(OutboxEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean markPublished(OutboxClaim claim) {
        var ownedEvent = findOwnedClaim(claim);
        if (ownedEvent.isEmpty()) {
            return false;
        }
        OutboxEvent event = ownedEvent.get();
        event.setStatus(OutboxStatus.PUBLISHED);
        event.setPublishedAt(Instant.now());
        event.setLastErrorCode(null);
        clearLease(event);
        return true;
    }

    @Transactional
    public void scheduleRetry(OutboxClaim claim) {

        var ownedEvent = findOwnedClaim(claim);

        if (ownedEvent.isEmpty()) {
            return;
        }

        OutboxEvent event = ownedEvent.get();

        event.setLastErrorCode("PUBLISH_ATTEMPT_FAILED");

        if (event.getAttempts() >= MAX_ATTEMPTS) {
            event.setStatus(OutboxStatus.FAILED);
        } else {
            long delaySeconds = Math.min(
                    300L,
                    5L << Math.min(event.getAttempts() - 1, 6));

            event.setStatus(OutboxStatus.PENDING);
            event.setNextAttemptAt(
                    Instant.now().plusSeconds(delaySeconds));
        }

        clearLease(event);
    }

    private Optional<OutboxEvent> findOwnedClaim(
            OutboxClaim outboxClaim) {
        var found = repository.findByIdForUpdate(
                outboxClaim.eventId());

        if (found.isEmpty()) {
            return Optional.empty();
        }

        OutboxEvent event = found.get();

        entityManager.refresh(event);

        boolean ownLease =
                event.getStatus() == OutboxStatus.PROCESSING
                && outboxClaim.leaseToken().equals(event.getLeaseToken())
                && event.getLeaseUntil() != null
                && event.getLeaseUntil().isAfter(Instant.now());

        return ownLease
                ? Optional.of(event)
                : Optional.empty();

    }

    private void clearLease(OutboxEvent event) {
        event.setLeaseToken(null);
        event.setLeaseUntil(null);
    }
}
