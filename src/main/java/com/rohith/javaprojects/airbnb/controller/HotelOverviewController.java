package com.rohith.javaprojects.airbnb.controller;

import com.rohith.javaprojects.airbnb.dto.HotelOverviewDTO;
import com.rohith.javaprojects.airbnb.service.HotelOverviewService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/admin/hotels")
public class HotelOverviewController {

    private final HotelOverviewService overviewService;

    public HotelOverviewController(
            HotelOverviewService overviewService) {

        this.overviewService = overviewService;
    }

    @GetMapping("/{hotelId}/overview")
    public CompletableFuture<HotelOverviewDTO> getOverview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long hotelId) {

        Long ownerId = Long.valueOf(jwt.getSubject());

        return overviewService.getOverview(
                ownerId,
                hotelId);
    }
}