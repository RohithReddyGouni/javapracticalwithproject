package com.rohith.javaprojects.airbnb.repository;

import com.rohith.javaprojects.airbnb.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO idempotency_records (
                user_id, operation, request_key,
                request_hash, status, created_at
            )
            VALUES (
                :userId, :operation, :requestKey,
                :requestHash, 'IN_PROGRESS', CURRENT_TIMESTAMP
            )
            ON CONFLICT (user_id, operation, request_key)
            DO NOTHING
            """, nativeQuery = true)
    int claim(
            @Param("userId") Long userId,
            @Param("operation") String operation,
            @Param("requestKey") String requestKey,
            @Param("requestHash") String requestHash);

    Optional<IdempotencyRecord> findByUser_IdAndOperationAndRequestKey(
            Long userId,
            String operation,
            String requestKey);
}
