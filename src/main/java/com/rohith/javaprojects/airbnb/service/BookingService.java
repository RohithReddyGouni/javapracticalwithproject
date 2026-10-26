package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.dto.BookingDTO;
import com.rohith.javaprojects.airbnb.dto.BookingRequestDTO;

public interface BookingService {
    BookingDTO createReservation(
            Long userId, String idempotencyKey, BookingRequestDTO request);

    BookingDTO getBookingById(Long userId, Long bookingId);

    BookingDTO cancelReservation(Long userId, Long bookingId);
}
