package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.dto.HotelDTO;
import com.rohith.javaprojects.airbnb.dto.RoomDTO;

import java.util.List;

public interface HotelService {
    HotelDTO createHotel(Long ownerId, HotelDTO hotelDTO);
    HotelDTO getHotelById(Long ownerId, Long hotelId);
    List<HotelDTO> getAllHotels(Long ownerId);
    HotelDTO updateHotelById(Long ownerId, Long hotelId, HotelDTO hotelDTO);
    void deleteHotelById(Long ownerId, Long hotelId);
    void activateHotel(Long ownerId, Long hotelId);



}
