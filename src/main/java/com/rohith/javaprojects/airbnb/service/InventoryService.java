package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.dto.InventoryDTO;
import com.rohith.javaprojects.airbnb.dto.InventoryInitRequestDTO;
import com.rohith.javaprojects.airbnb.dto.InventoryUpdateRequestDTO;

import java.time.LocalDate;
import java.util.List;

public interface InventoryService {

    void initializeInventory(Long ownerId,
                             Long hotelId,
                             Long roomId,
                             InventoryInitRequestDTO request);

    List<InventoryDTO> getInventory(
            Long ownerId,
            Long hotelId,
            Long roomId,
            LocalDate startDate,
            LocalDate endDate);

    InventoryDTO updateInventory(
            Long ownerId,
            Long hotelId,
            Long roomId,
            LocalDate date,
            InventoryUpdateRequestDTO request
    );
}
