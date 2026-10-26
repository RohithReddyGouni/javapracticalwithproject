package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.dto.WebhookClaim;
import com.rohith.javaprojects.airbnb.entity.StripeWebhookEvent;
import com.rohith.javaprojects.airbnb.entity.enums.WebhookFailureCode;
import com.rohith.javaprojects.airbnb.entity.enums.WebhookStatus;
import com.rohith.javaprojects.airbnb.repository.StripeWebhookEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class WebhookClaimService {

    private final StripeWebhookEventRepository repository;

    public WebhookClaimService(
            StripeWebhookEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<WebhookClaim> claimBatch() {

        Instant now = Instant.now();

        List<StripeWebhookEvent> events =
                repository.findClaimableEvents(now, 10);

        List<WebhookClaim> claims = new ArrayList<>();

        for (StripeWebhookEvent event : events) {
            if (event.getAttempts() >= 5) {
                event.setStatus(WebhookStatus.FAILED);
                event.setLastErrorCode(
                        WebhookFailureCode.PROCESSING_ATTEMPTS_EXHAUSTED.name());
                event.setLeaseToken(null);
                event.setLeaseUntil(null);
                continue;
            }
            UUID token = UUID.randomUUID();

            event.setStatus(WebhookStatus.PROCESSING);
            event.setLeaseToken(token);
            event.setLeaseUntil(now.plusSeconds(120));
            event.setAttempts(event.getAttempts() + 1);

            claims.add(new WebhookClaim(
                    event.getEventId(),
                    token,
                    event.getEventType(),
                    event.getPayload()));
        }

        return claims;
    }
}