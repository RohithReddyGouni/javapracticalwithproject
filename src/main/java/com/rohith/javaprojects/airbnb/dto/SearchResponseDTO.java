package com.rohith.javaprojects.airbnb.dto;

import java.util.List;

public record SearchResponseDTO(
        List<HotelSearchResultDTO> content,
        int page,
        int size,
        boolean hasNext
) {
}
