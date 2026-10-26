package com.rohith.javaprojects.airbnb.repository;

import com.rohith.javaprojects.airbnb.entity.Inventory;
import com.rohith.javaprojects.airbnb.repository.projection.HotelSearchProjection;
import jakarta.persistence.LockModeType;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    boolean existsByRoom_IdAndDateGreaterThanEqualAndDateLessThan(
            Long roomId,
            LocalDate startDate,
            LocalDate endDate);

    List<Inventory> findByRoom_IdAndDateGreaterThanEqualAndDateLessThanOrderByDateAsc(
            Long roomId,
            LocalDate startDate,
            LocalDate endDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i 
            FROM Inventory i
            WHERE i.room.id = :roomId
                AND i.date = :date
            """)
    Optional<Inventory> findForUpdate(
           @Param("roomId") Long roomId,
           @Param ("date") LocalDate date
    );

    boolean existsByRoom_Id(Long roomId);

    @Query("""
            SELECT h.id AS hotelId,
                   h.name AS hotelName,
                   h.city AS city,
                   r.id AS roomId,
                   r.type AS roomType,
                   r.capacity AS capacity,
                   SUM(i.price) * :roomsCount AS totalPrice
            FROM Inventory i
            JOIN i.room r 
            JOIN r.hotel h
            WHERE h.active = true
                AND LOWER(h.city) = LOWER(:city)
                AND r.capacity >= :guestsPerRoom
                AND i.date >= :checkInDate
                AND i.date < :checkOutDate
                AND (i.totalCount - i.bookedCount - i.reservedCount)
                    >= :roomsCount
           GROUP BY h.id, h.name, h.city,
                    r.id, r.type, r.capacity
           HAVING COUNT(r.id) = :nights
           ORDER BY SUM(i.price) * :roomsCount ASC,
                        h.id ASC, r.id ASC
            """)
    Slice<HotelSearchProjection> searchAvailableRooms(
            @Param("city") String city,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkoutDate,
            @Param("roomsCount") Integer roomsCount,
            @Param("guestsPerRoom") Integer guestsPerRoom,
            @Param("nights") Long nights,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i
            FROM Inventory i
            WHERE i.room.id = :roomId
                AND i.date >= :checkInDate
                AND i.date < :checkOutDate
            ORDER BY i.date ASC
            """)
    List<Inventory> findStayInventoryForUpdate(
           @Param("roomId") Long roomId,
           @Param("checkInDate") LocalDate checkInDate,
           @Param("checkOutDate") LocalDate checkOutDate
    );
}
