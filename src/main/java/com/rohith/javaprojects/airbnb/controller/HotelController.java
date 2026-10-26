package com.rohith.javaprojects.airbnb.controller;


import com.rohith.javaprojects.airbnb.dto.HotelDTO;
import com.rohith.javaprojects.airbnb.service.HotelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/hotels")
public class HotelController {

    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @PostMapping
    public ResponseEntity<HotelDTO> createHotel(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody HotelDTO hotelDTO) {
        Long userId = Long.valueOf(jwt.getSubject());
        HotelDTO createdHotel = hotelService.createHotel(userId, hotelDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(createdHotel);

    }

    @GetMapping("/{hotelId}")
    public ResponseEntity<HotelDTO> getHotelById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long hotelId) {
        Long ownerId = Long.valueOf(jwt.getSubject());
        return ResponseEntity.ok(
                hotelService.getHotelById(ownerId, hotelId)
        );
    }

    @GetMapping
    public ResponseEntity<List<HotelDTO>> getAllHotels(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long ownerId = Long.valueOf(jwt.getSubject());
        return ResponseEntity.ok(
                hotelService.getAllHotels(ownerId));

    }


    @PutMapping("/{hotelId}")
    public ResponseEntity<HotelDTO> updateHotel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long hotelId,
           @Valid @RequestBody HotelDTO hotelDTO) {
        Long ownerId = Long.valueOf(jwt.getSubject());
        return ResponseEntity.ok(
                hotelService.updateHotelById(ownerId, hotelId, hotelDTO)
        );
    }

    @DeleteMapping("/{hotelId}")
    public ResponseEntity<Void> updateHotel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long hotelId) {
        Long ownerId = Long.valueOf(jwt.getSubject());
        hotelService.deleteHotelById(ownerId, hotelId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{hotelId}/activate")
    public ResponseEntity<Void> activateHotel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long hotelId) {
        Long ownerId = Long.valueOf(jwt.getSubject());
        hotelService.activateHotel(ownerId, hotelId);
        return ResponseEntity.noContent().build();
    }
}
