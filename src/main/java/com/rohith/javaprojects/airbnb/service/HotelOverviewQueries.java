package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.entity.enums.BookingStatus;
import com.rohith.javaprojects.airbnb.repository.BookingRepository;
import com.rohith.javaprojects.airbnb.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, timeout = 2)
public class HotelOverviewQueries {

    private static final Logger log =
            LoggerFactory.getLogger(HotelOverviewQueries.class);

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;

    public HotelOverviewQueries(
            RoomRepository roomRepository,
            BookingRepository bookingRepository) {

        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
    }

    public long countRoomCategories(Long ownerId, Long hotelId) {

        log.info(
                "Counting room categories: hotelId={} thread={}",
                hotelId,
                Thread.currentThread().getName());

        return roomRepository.countByHotel_IdAndHotel_Owner_Id(
                hotelId,
                ownerId);
    }

    public long countConfirmedBookings(Long ownerId, Long hotelId) {

        log.info(
                "Counting confirmed bookings: hotelId={} thread={}",
                hotelId,
                Thread.currentThread().getName());

        return bookingRepository
                .countByRoom_Hotel_IdAndRoom_Hotel_Owner_IdAndStatus(
                        hotelId,
                        ownerId,
                        BookingStatus.CONFIRMED);
    }
}