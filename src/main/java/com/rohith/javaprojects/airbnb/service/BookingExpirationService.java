package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.entity.Booking;
import com.rohith.javaprojects.airbnb.entity.Inventory;
import com.rohith.javaprojects.airbnb.entity.enums.BookingStatus;
import com.rohith.javaprojects.airbnb.exception.ResourceConflictException;
import com.rohith.javaprojects.airbnb.exception.ResourceNotFoundException;
import com.rohith.javaprojects.airbnb.repository.BookingRepository;
import com.rohith.javaprojects.airbnb.repository.InventoryRepository;
import com.rohith.javaprojects.airbnb.repository.RoomRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BookingExpirationService {

    private final BookingRepository bookingRepository;

    private final RoomRepository roomRepository;

    private final InventoryRepository inventoryRepository;


    public BookingExpirationService(BookingRepository bookingRepository, RoomRepository roomRepository, InventoryRepository inventoryRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void expireReservation(Long bookingId) {
        Booking initialBooking = bookingRepository.findById(bookingId)
                .orElse(null);

        if (initialBooking == null) {
            return;
        }

        Long roomId = initialBooking.getRoom().getId();

        roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Room missing for booking:: "+ bookingId));

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking disappeared during expiration"));

        entityManager.refresh(booking);

        if (booking.getStatus() != BookingStatus.RESERVED
                || booking.getReservedUntil().isAfter(Instant.now())) {
            return;
        }

        List<Inventory> rows = inventoryRepository
                .findStayInventoryForUpdate(
                        roomId,
                        booking.getCheckInDate(),
                        booking.getCheckOutDate());

        long nights = ChronoUnit.DAYS.between(
                booking.getCheckInDate(),
                booking.getCheckOutDate());

        if (rows.size() != nights) {
            throw new ResourceConflictException(
                    "Inventory is incomplete for booking: " + bookingId);
        }

        for (Inventory inventory : rows) {
            if (inventory.getReservedCount() < booking.getRoomsCount()) {
                throw new ResourceConflictException(
                        "Inventory counts are inconsistent for booking: "
                                + bookingId);
            }
        }

        for (Inventory inventory : rows) {
            inventory.setReservedCount(
                    inventory.getReservedCount() - booking.getRoomsCount());
        }

        booking.setStatus(BookingStatus.EXPIRED);

    }
}
