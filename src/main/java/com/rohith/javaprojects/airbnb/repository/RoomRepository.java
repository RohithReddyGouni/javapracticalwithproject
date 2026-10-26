package com.rohith.javaprojects.airbnb.repository;

import com.rohith.javaprojects.airbnb.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByHotel_Id(Long hotelId);
    Optional<Room> findByIdAndHotel_Id(Long roomId, Long hotelId);
    boolean existsByHotel_Id(Long hotelId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r from Room r
            where r.id = :roomId
            AND r.hotel.id = :hotelId
            """)
    Optional<Room> findForUpdate(
           @Param("roomId") Long roomId,
            @Param("hotelId") Long hotelId
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r
            FROM Room r
            WHERE r.id = :roomId
            """)
    Optional<Room> findByIdForUpdate(@Param("roomId") Long roomId);
    long countByHotel_IdAndHotel_Owner_Id(
            Long hotelId,
            Long ownerId);

}
