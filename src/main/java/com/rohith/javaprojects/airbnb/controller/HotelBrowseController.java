package com.rohith.javaprojects.airbnb.controller;


import com.rohith.javaprojects.airbnb.dto.HotelSearchRequestDTO;
import com.rohith.javaprojects.airbnb.dto.HotelSearchResultDTO;
import com.rohith.javaprojects.airbnb.dto.SearchResponseDTO;
import com.rohith.javaprojects.airbnb.service.HotelSearchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/hotels")
public class HotelBrowseController {

    private final HotelSearchService hotelSearchService;

    public HotelBrowseController(HotelSearchService hotelSearchService) {
        this.hotelSearchService = hotelSearchService;
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponseDTO> searchHotels(
           @Valid @ModelAttribute HotelSearchRequestDTO request) {
        return ResponseEntity.ok(
                hotelSearchService.searchHotels(request));

    }
}
