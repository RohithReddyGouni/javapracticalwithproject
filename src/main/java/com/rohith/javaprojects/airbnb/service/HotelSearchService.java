package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.dto.HotelSearchRequestDTO;
import com.rohith.javaprojects.airbnb.dto.HotelSearchResultDTO;
import com.rohith.javaprojects.airbnb.dto.SearchResponseDTO;

import java.util.List;

public interface HotelSearchService {
    SearchResponseDTO searchHotels(
            HotelSearchRequestDTO request);
}
