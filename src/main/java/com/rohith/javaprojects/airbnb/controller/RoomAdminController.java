package com.rohith.javaprojects.airbnb.controller;


import com.rohith.javaprojects.airbnb.dto.RoomDTO;
import com.rohith.javaprojects.airbnb.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/hotels/{hotelId}/rooms")
public class RoomAdminController {

    private final RoomService roomService;

    public RoomAdminController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<RoomDTO> createRoom(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long hotelId,
            @Valid @RequestBody RoomDTO roomDTO) {
        Long ownerId = Long.valueOf(jwt.getSubject());
        RoomDTO createdRoom = roomService.createRoom(ownerId, hotelId, roomDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(createdRoom);

    }

    @GetMapping
    public ResponseEntity<List<RoomDTO>> getRoomsByHotelId(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long hotelId) {
        Long ownerId = Long.valueOf(jwt.getSubject());
        List<RoomDTO> response = roomService.getRoomsByHotelId(ownerId, hotelId);

        return ResponseEntity.ok(response);

    }


    @GetMapping("/{roomId}")
    public ResponseEntity<RoomDTO> getRoomsById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long hotelId, @PathVariable Long roomId) {
        Long ownerId = Long.valueOf(jwt.getSubject());
        RoomDTO response = roomService.getRoomById(ownerId, hotelId, roomId);

        return ResponseEntity.ok(response);

    }

    @PutMapping("/{roomId}")
    public ResponseEntity<RoomDTO> updateRoomById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long hotelId,
            @PathVariable Long roomId,
            @Valid @RequestBody RoomDTO roomDTO) {
        Long ownerId = Long.valueOf(jwt.getSubject());
        return ResponseEntity.ok(
                roomService.updateRoomById(ownerId, hotelId, roomId, roomDTO));
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoomById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long hotelId,
            @PathVariable Long roomId) {
        Long ownerId = Long.valueOf(jwt.getSubject());

        roomService.deleteRoomById(ownerId, hotelId, roomId);
        return ResponseEntity.noContent().build();
    }
}
