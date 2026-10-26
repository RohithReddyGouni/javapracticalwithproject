package com.rohith.javaprojects.airbnb.repository;


import com.rohith.javaprojects.airbnb.entity.Booking;
import com.rohith.javaprojects.airbnb.entity.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByIdAndUser_Id(Long bookingId, Long userId);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b FROM Booking b
            WHERE b.id = :bookingId
                AND b.user.id = :userId
            """)
    Optional<Booking> findOwnedBookingForUpdate(
            @Param("bookingId") Long bookingId,
            @Param("userId") Long userId);


    @Query("""
            SELECT b.id
            FROM Booking b
            WHERE b.status = :status
                AND b.reservedUntil <= :now
                ORDER By b.reservedUntil ASC, b.id ASC
            """)
    List<Long> findExpirationCandidates(
           @Param("status") BookingStatus status,
            @Param("now") Instant now,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT b FROM Booking b
            WHERE b.id = :bookingId
            """
            )
    Optional<Booking> findByIdForUpdate(
            @Param("bookingId") Long bookingId);

    long countByRoom_Hotel_IdAndRoom_Hotel_Owner_IdAndStatus(
            Long hotelId,
            Long ownerId,
            BookingStatus status);

}
