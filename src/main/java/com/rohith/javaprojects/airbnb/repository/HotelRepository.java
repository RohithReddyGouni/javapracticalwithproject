package com.rohith.javaprojects.airbnb.repository;

import com.rohith.javaprojects.airbnb.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
    List<Hotel> findByOwner_Id(Long ownerId);
    Optional<Hotel> findByIdAndOwner_Id(Long hotelId, Long ownerId);
    boolean existsByIdAndOwner_Id(Long hotelId, Long ownerId);
}
