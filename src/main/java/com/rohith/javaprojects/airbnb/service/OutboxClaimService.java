package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.dto.OutboxClaim;
import com.rohith.javaprojects.airbnb.entity.OutboxEvent;
import com.rohith.javaprojects.airbnb.entity.enums.OutboxStatus;
import com.rohith.javaprojects.airbnb.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class OutboxClaimService {

    private static final int MAX_ATTEMPTS = 10;
    private static final long LEASE_SECONDS = 120;

    private final OutboxEventRepository repository;

    public OutboxClaimService(OutboxEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Optional<OutboxClaim> claimNext() {

        Instant now = Instant.now();

        var events = repository.findNextClaimable(now);

        if (events.isEmpty()) {
            return Optional.empty();
        }

        OutboxEvent event = events.get(0);

        if (event.getAttempts() >= MAX_ATTEMPTS) {
            event.setStatus(OutboxStatus.FAILED);
            event.setLastErrorCode(
                    "PUBLISH_ATTEMPTS_EXHAUSTED");
            event.setLeaseToken(null);
            event.setLeaseUntil(null);

            return Optional.empty();
        }

        UUID leaseToken = UUID.randomUUID();

        event.setStatus(OutboxStatus.PROCESSING);
        event.setAttempts(event.getAttempts() + 1);
        event.setLeaseToken(leaseToken);
        event.setLeaseUntil(now.plusSeconds(LEASE_SECONDS));

        String messageKey =
                event.getAggregateType()
                        + ":"
                        + event.getAggregateId();

        return Optional.of(new OutboxClaim(
                event.getId(),
                leaseToken,
                messageKey,
                event.getPayload()));
    }
}