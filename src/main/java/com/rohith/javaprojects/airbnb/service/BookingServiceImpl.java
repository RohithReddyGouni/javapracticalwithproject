package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.dto.BookingDTO;
import com.rohith.javaprojects.airbnb.dto.BookingRequestDTO;
import com.rohith.javaprojects.airbnb.entity.*;
import com.rohith.javaprojects.airbnb.entity.enums.BookingStatus;
import com.rohith.javaprojects.airbnb.entity.enums.IdempotencyStatus;
import com.rohith.javaprojects.airbnb.exception.InvalidRequestException;
import com.rohith.javaprojects.airbnb.exception.ResourceConflictException;
import com.rohith.javaprojects.airbnb.exception.ResourceNotFoundException;
import com.rohith.javaprojects.airbnb.repository.*;

import com.rohith.javaprojects.airbnb.util.BookingRequestFingerPrint;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BookingServiceImpl implements BookingService{

    private final InventoryRepository inventoryRepository;

    private final UserRepository userRepository;

    private final RoomRepository roomRepository;

    private final BookingRepository bookingRepository;

    private final ModelMapper modelMapper;

    private final IdempotencyRepository idempotencyRepository;

    private final BookingRequestFingerPrint bookingRequestFingerPrint;

    @PersistenceContext
    private EntityManager entityManager;

    public BookingServiceImpl(InventoryRepository inventoryRepository, UserRepository userRepository, RoomRepository roomRepository, BookingRepository bookingRepository, ModelMapper modelMapper, IdempotencyRepository idempotencyRepository, BookingRequestFingerPrint bookingRequestFingerPrint) {
        this.inventoryRepository = inventoryRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.modelMapper = modelMapper;
        this.idempotencyRepository = idempotencyRepository;
        this.bookingRequestFingerPrint = bookingRequestFingerPrint;
    }

    @Override
    @Transactional
    public BookingDTO createReservation(Long userId, String idempotencyKey, BookingRequestDTO request) {

        if (idempotencyKey == null ||
                !idempotencyKey.matches("[A-Za-z0-9._:-]{1,100}")) {
            throw new InvalidRequestException(
                    "Idempotency-Key must contain 1–100 letters, digits, dots, underscores, colons or hyphens");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID:: "+userId
                ));

        String operation = "CREATE_RESERVATION";
        String requestHash = bookingRequestFingerPrint.calculate(request);

        int claimed = idempotencyRepository.claim(userId, operation, idempotencyKey, requestHash);

        IdempotencyRecord record = idempotencyRepository
                .findByUser_IdAndOperationAndRequestKey(
                        userId, operation, idempotencyKey)
                .orElseThrow(() -> new IllegalStateException(
                        "Idempotency record is missing"));
        if (!record.getRequestHash().equals(requestHash)) {
            throw new ResourceConflictException(
                    "Idempotency-Key was already used with different booking details");
        }

        if (claimed == 0) {
            if (record.getStatus() != IdempotencyStatus.COMPLETED || record.getBooking() == null) {
                throw new ResourceConflictException(
                        "Reservation request has not completed");
            }
            return modelMapper.map(record.getBooking(), BookingDTO.class);
        }

        LocalDate checkIn = request.getCheckInDate();
        LocalDate checkOut = request.getCheckOutDate();

        if (checkIn == null || checkOut == null) {
            throw new InvalidRequestException(
                    "Check-in and Check-out dates are required"
            );
        }

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);

        if (checkIn.isBefore(LocalDate.now())
                || nights < 1 || nights > 365 ) {
            throw new InvalidRequestException(
                    "Stay must start today or later and contain 1-365 nights"
            );
        }


        Room room = roomRepository.findByIdForUpdate(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Room not found with ID:: "+ request.getRoomId()
                ));

        if (!room.getHotel().isActive()) {
            throw new ResourceConflictException(
                    "Hotel is not accepting reservations");
        }

        if (request.getGuestsPerRoom() > room.getCapacity()) {
            throw new InvalidRequestException(
                    "Guests per room exceed this category's capacity");
        }

        List<Inventory> inventoryRows = inventoryRepository
                .findStayInventoryForUpdate(
                        room.getId(), checkIn, checkOut
                );

        if (inventoryRows.size() != nights) {
            throw new ResourceConflictException(
                    "Inventory is missing for part of the stay");
        }

        int roomsCount = request.getRoomsCount();
        BigDecimal nightlyPriceSum = BigDecimal.ZERO;

        for(Inventory inventory : inventoryRows) {
            int available = inventory.getTotalCount()
                    - inventory.getBookedCount()
                    - inventory.getReservedCount();
            if (available < roomsCount) {
                throw new ResourceConflictException(
                        "Not enough rooms available for the entire stay");
            }

            nightlyPriceSum = nightlyPriceSum.add(inventory.getPrice());
        }
        BigDecimal totalPrice = nightlyPriceSum.multiply(
                    BigDecimal.valueOf(roomsCount));

        if(totalPrice.compareTo(new BigDecimal("999999999999.99")) > 0) {
            throw new InvalidRequestException(
                    "Reservation total exceeds the supported amount");
        }

        for (Inventory inventory: inventoryRows) {
            inventory.setReservedCount(
                    inventory.getReservedCount()+ roomsCount);
        }

        Instant now = Instant.now();
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoom(room);
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setRoomsCount(roomsCount);
        booking.setGuestsPerRoom(request.getGuestsPerRoom());
        booking.setTotalPrice(totalPrice);
        booking.setCurrency("USD");
        booking.setStatus(BookingStatus.RESERVED);
        booking.setCreatedAt(now);
        booking.setReservedUntil(now.plusSeconds(900));

        Booking savedBooking = bookingRepository.save(booking);
        record.setBooking(savedBooking);
        record.setStatus(IdempotencyStatus.COMPLETED);

        return modelMapper.map(savedBooking, BookingDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDTO getBookingById(Long userId, Long bookingId) {
        Booking booking = bookingRepository
                .findByIdAndUser_Id(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with ID:: "+ bookingId));

        return modelMapper.map(booking, BookingDTO.class);
    }

    @Override
    @Transactional
    public BookingDTO cancelReservation(Long userId, Long bookingId) {
        Booking initialBooking = bookingRepository
                .findByIdAndUser_Id(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with ID:: "+bookingId));
        Long roomId = initialBooking.getRoom().getId();

        roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Room not found with ID:: "+roomId
                ));

        Booking booking = bookingRepository
                .findOwnedBookingForUpdate(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with ID:: "+bookingId));

        entityManager.refresh(booking);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return modelMapper.map(booking, BookingDTO.class);
        }

        if (booking.getStatus() != BookingStatus.RESERVED) {
            throw new ResourceConflictException(
                    "Only unpaid reservations can be cancelled through this operation");
        }

        List<Inventory> rows = inventoryRepository
                .findStayInventoryForUpdate(
                        roomId,
                        booking.getCheckInDate(),
                        booking.getCheckOutDate());

        long nights = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());

        if (rows.size() != nights) {
            throw new ResourceConflictException(
                    "Inventory is incomplete; cancellation cannot be completed");
        }

        for(Inventory inventory: rows) {
            if (inventory.getReservedCount() < booking.getRoomsCount()) {
                throw new ResourceConflictException(
                        "Inventory counts are inconsistent; cancellation cannot be completed");
            }
        }

        for(Inventory inventory : rows) {
            inventory.setReservedCount(
                    inventory.getReservedCount() - booking.getRoomsCount());
        }
        booking.setStatus(BookingStatus.CANCELLED);
        return modelMapper.map(booking, BookingDTO.class);
    }
}
