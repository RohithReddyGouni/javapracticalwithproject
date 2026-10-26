package com.rohith.javaprojects.airbnb.repository;

import com.rohith.javaprojects.airbnb.entity.OutboxEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, UUID> {

    @Query(value = """
            SELECT *
            FROM outbox_events
            WHERE aggregate_type = 'BOOKING'
              AND (
                  (status = 'PENDING'
                      AND next_attempt_at <= :now)
                  OR
                  (status = 'PROCESSING'
                      AND lease_until <= :now)
              )
            ORDER BY created_at ASC, id ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> findNextClaimable(
            @Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e
            FROM OutboxEvent e
            WHERE e.id = :id
            """)
    Optional<OutboxEvent> findByIdForUpdate(
            @Param("id") UUID id);
}