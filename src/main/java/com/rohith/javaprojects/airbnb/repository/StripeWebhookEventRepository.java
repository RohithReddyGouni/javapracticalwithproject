package com.rohith.javaprojects.airbnb.repository;

import com.rohith.javaprojects.airbnb.entity.StripeWebhookEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StripeWebhookEventRepository
        extends JpaRepository<StripeWebhookEvent, String> {

    @Modifying
    @Query(value = """
            INSERT INTO stripe_webhook_events (
                event_id,
                event_type,
                payload,
                status,
                attempts,
                received_at,
                next_attempt_at
            )
            VALUES (
                :eventId,
                :eventType,
                :payload,
                'PENDING',
                0,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            )
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("eventId") String eventId,
            @Param("eventType") String eventType,
            @Param("payload") String payload);

    @Query(value = """
        SELECT *
        FROM stripe_webhook_events
        WHERE (
            status = 'PENDING'
            AND next_attempt_at <= :now
        )
        OR (
            status = 'PROCESSING'
            AND lease_until <= :now
        )
        ORDER BY received_at ASC, event_id ASC
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<StripeWebhookEvent> findClaimableEvents(
            @Param("now") Instant now,
            @Param("batchSize") int batchSize);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT e FROM StripeWebhookEvent e
        WHERE e.eventId = :eventId
        """)
    Optional<StripeWebhookEvent> findByEventIdForUpdate(
            @Param("eventId") String eventId);
}