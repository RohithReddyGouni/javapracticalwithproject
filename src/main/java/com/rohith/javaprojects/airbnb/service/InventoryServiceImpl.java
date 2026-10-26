package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.dto.InventoryDTO;
import com.rohith.javaprojects.airbnb.dto.InventoryInitRequestDTO;
import com.rohith.javaprojects.airbnb.dto.InventoryUpdateRequestDTO;
import com.rohith.javaprojects.airbnb.entity.Inventory;
import com.rohith.javaprojects.airbnb.entity.Room;
import com.rohith.javaprojects.airbnb.exception.InvalidRequestException;
import com.rohith.javaprojects.airbnb.exception.ResourceConflictException;
import com.rohith.javaprojects.airbnb.exception.ResourceNotFoundException;
import com.rohith.javaprojects.airbnb.repository.HotelRepository;
import com.rohith.javaprojects.airbnb.repository.InventoryRepository;
import com.rohith.javaprojects.airbnb.repository.RoomRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class InventoryServiceImpl implements InventoryService{

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;

    public InventoryServiceImpl(HotelRepository hotelRepository,
                                RoomRepository roomRepository,
                                InventoryRepository inventoryRepository) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    @Transactional
    public void initializeInventory(
            Long ownerId,
            Long hotelId,
            Long roomId,
            InventoryInitRequestDTO request) {

        hotelRepository.findByIdAndOwner_Id(hotelId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Hotel not found with ID:: "+hotelId
                ));
        Room room = roomRepository.findForUpdate(roomId, hotelId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Room not found with ID:: "+roomId
                ));

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        if (startDate == null || endDate == null) {
            throw new InvalidRequestException(
                    "Start date and end date are required");
        }

        long nights = ChronoUnit.DAYS.between(startDate, endDate);
        if (startDate.isBefore(LocalDate.now())
            || nights < 1 || nights > 365) {
            throw new InvalidRequestException(
                    "Date range must start today or later and contain 1-365 nights"
            );
        }
        boolean overlaps = inventoryRepository.existsByRoom_IdAndDateGreaterThanEqualAndDateLessThan(
                roomId, startDate, endDate);

        if (overlaps) {
            throw new ResourceConflictException(
                    "Inventory already exists within the requested date range");
        }

        List<Inventory> rows = new ArrayList<>();
        for (LocalDate date = startDate;
             date.isBefore(endDate);
             date = date.plusDays(1)) {
            Inventory inventory = new Inventory();
            inventory.setRoom(room);
            inventory.setDate(date);
            inventory.setTotalCount(room.getTotalCount());
            inventory.setBookedCount(0);
            inventory.setReservedCount(0);
            inventory.setPrice(room.getBasePrice());
            rows.add(inventory);
        }
     inventoryRepository.saveAll(rows);

    }

    public List<InventoryDTO> getInventory(
            Long ownerId,
            Long hotelId,
            Long roomId,
            LocalDate startDate,
            LocalDate endDate) {
        hotelRepository.findByIdAndOwner_Id(hotelId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Hotel not found with ID:: "+hotelId
                ));
        Room room = roomRepository.findByIdAndHotel_Id(roomId, hotelId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Room not found with ID:: "+roomId
                ));

        if (startDate == null || endDate == null) {
            throw new InvalidRequestException(
                    "Start date and end date are required");
        }

        long nights = ChronoUnit.DAYS.between(startDate, endDate);
        if (startDate.isBefore(LocalDate.now())
                || nights < 1 || nights > 365) {
            throw new InvalidRequestException(
                    "Date range must start today or later and contain 1-365 nights"
            );
        }

        List<Inventory> inventoryList = inventoryRepository.findByRoom_IdAndDateGreaterThanEqualAndDateLessThanOrderByDateAsc(
                roomId,
                startDate,
                endDate);
        List<InventoryDTO> response = new ArrayList<>();
        for(Inventory inventory: inventoryList) {

            response.add(toDto(inventory));
        }
        return response;
    }

    @Override
    @Transactional
    public InventoryDTO updateInventory(
            Long ownerId,
            Long hotelId,
            Long roomId,
            LocalDate date,
            InventoryUpdateRequestDTO request) {
        hotelRepository.findByIdAndOwner_Id(hotelId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Hotel not found with ID:: "+ hotelId
                ));

        Room room = roomRepository.findForUpdate(roomId, hotelId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Room not found with ID:: "+ roomId
                ));

        if (date == null || date.isBefore(LocalDate.now())) {
            throw new InvalidRequestException(
                    "Inventory date must be today or later");
        }

        Inventory inventory = inventoryRepository
                .findForUpdate(roomId, date)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for date: " + date
                ));

        int requestedTotal = getRequestedTotal(request, room, inventory);

        inventory.setTotalCount(requestedTotal);
        inventory.setPrice(request.getPrice());

        return toDto(inventory);
    }

    private static int getRequestedTotal(InventoryUpdateRequestDTO request, Room room, Inventory inventory) {
        int requestedTotal = request.getTotalCount();

        if (requestedTotal > room.getTotalCount()) {
            throw new InvalidRequestException(
                    "Inventory capacity cannot exceed the room category's total count"
            );
        }

        long committedCount = (long) inventory.getBookedCount() + inventory.getReservedCount();
        if (requestedTotal < committedCount) {
            throw new ResourceConflictException(
                    "Inventory capacity cannot be less than booked and reserved rooms");
        }
        return requestedTotal;
    }

    private InventoryDTO toDto(Inventory inventory) {
        InventoryDTO dto = new InventoryDTO();
        dto.setId(inventory.getId());
        dto.setDate(inventory.getDate());
        dto.setTotalCount(inventory.getTotalCount());
        dto.setBookedCount(inventory.getBookedCount());
        dto.setReservedCount(inventory.getReservedCount());
        dto.setAvailableCount(inventory.getTotalCount()
                        - inventory.getReservedCount()
                        - inventory.getBookedCount());
        dto.setPrice(inventory.getPrice());

        return dto;
    }
}
