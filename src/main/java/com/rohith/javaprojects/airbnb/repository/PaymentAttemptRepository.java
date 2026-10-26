package com.rohith.javaprojects.airbnb.repository;

import com.rohith.javaprojects.airbnb.entity.PaymentAttempt;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    Optional<PaymentAttempt> findByAttemptKey(UUID attemptKey);

    Optional<PaymentAttempt> findByStripeSessionId(
            String stripeSessionId);

    Optional<PaymentAttempt> findFirstByBooking_IdOrderByIdDesc(
            Long bookingId);

    Optional<PaymentAttempt> findByAttemptKeyAndBooking_User_Id(
            UUID attemptKey, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p FROM PaymentAttempt p
        WHERE p.attemptKey = :attemptKey
          AND p.booking.user.id = :userId
        """)
    Optional<PaymentAttempt> findOwnedAttemptForUpdate(
            @Param("attemptKey") UUID attemptKey,
            @Param("userId") Long userId);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p FROM PaymentAttempt p
        WHERE p.attemptKey = :attemptKey
        """)
    Optional<PaymentAttempt> findByAttemptKeyForUpdate(
            @Param("attemptKey") UUID attemptKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentAttempt p WHERE p.id = :id")
    Optional<PaymentAttempt> findByIdForUpdate(@Param("id") Long id);
}
