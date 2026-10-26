package com.rohith.javaprojects.airbnb.controller;


import com.rohith.javaprojects.airbnb.dto.InventoryDTO;
import com.rohith.javaprojects.airbnb.dto.InventoryInitRequestDTO;
import com.rohith.javaprojects.airbnb.dto.InventoryUpdateRequestDTO;
import com.rohith.javaprojects.airbnb.service.InventoryService;
import jakarta.validation.Valid;
import org.apache.coyote.Response;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/admin/hotels/{hotelId}/rooms/{roomId}/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ResponseEntity<Void> initializeInventory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long hotelId,
            @PathVariable Long roomId,
            @Valid @RequestBody InventoryInitRequestDTO request
            ) {
        Long ownerId = Long.valueOf(jwt.getSubject());
        inventoryService.initializeInventory(ownerId, hotelId, roomId, request);

        return ResponseEntity.status(201).build();

    }

    @GetMapping
    public ResponseEntity<List<InventoryDTO>> getInventory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long hotelId,
            @PathVariable Long roomId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {
        Long ownerId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok(
                inventoryService.getInventory(
                        ownerId,hotelId, roomId, startDate, endDate)
        );
    }


    @PutMapping("/{date}")
    public ResponseEntity<InventoryDTO> updateInventory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long hotelId,
            @PathVariable Long roomId,
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @Valid @RequestBody
    InventoryUpdateRequestDTO request) {

        Long ownerId = Long.valueOf(jwt.getSubject());
        return ResponseEntity.ok(inventoryService.updateInventory(ownerId,hotelId, roomId, date,request));
    }
}
