package com.rohith.javaprojects.airbnb.repository;

import com.rohith.javaprojects.airbnb.entity.RefundRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundRequestRepository
        extends JpaRepository<RefundRequest, Long> {

    Optional<RefundRequest> findByPaymentAttempt_Id(
            Long paymentAttemptId);

    @Modifying
    @Query(value = """
            INSERT INTO refund_requests (
                refund_key,
                payment_attempt_id,
                amount_minor,
                currency,
                status,
                attempts,
                next_attempt_at,
                created_at,
                updated_at
            )
            VALUES (
                :refundKey,
                :paymentAttemptId,
                :amountMinor,
                :currency,
                'PENDING',
                0,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            )
            ON CONFLICT (payment_attempt_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("refundKey") UUID refundKey,
            @Param("paymentAttemptId") Long paymentAttemptId,
            @Param("amountMinor") Long amountMinor,
            @Param("currency") String currency);

    @Query(value = """
        SELECT *
        FROM refund_requests
        WHERE status = 'PENDING'
          AND next_attempt_at <= :now
        ORDER BY created_at ASC, id ASC
        LIMIT 1
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<RefundRequest> findNextPending(@Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RefundRequest r WHERE r.id = :id")
    Optional<RefundRequest> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query(value = """
        UPDATE refund_requests
        SET status = 'RECONCILIATION_REQUIRED',
            last_error_code = 'WORKER_LEASE_EXPIRED',
            lease_token = NULL,
            lease_until = NULL,
            updated_at = :now
        WHERE status = 'PROCESSING'
          AND lease_until <= :now
        """, nativeQuery = true)
    int recoverExpiredLeases(@Param("now") Instant now);
}