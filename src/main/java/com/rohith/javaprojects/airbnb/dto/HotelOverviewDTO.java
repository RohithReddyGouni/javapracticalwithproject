package com.rohith.javaprojects.airbnb.dto;

public record HotelOverviewDTO(
        Long hotelId,
        long roomCategories,
        long confirmedBookings
) {
}