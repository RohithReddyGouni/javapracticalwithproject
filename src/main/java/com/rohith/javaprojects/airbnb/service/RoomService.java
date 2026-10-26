package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.dto.RoomDTO;

import java.util.List;

public interface RoomService {
    RoomDTO createRoom(Long ownerId, Long hotelId, RoomDTO roomDTO);
    List<RoomDTO> getRoomsByHotelId(Long ownerId, Long hotelId);
    RoomDTO getRoomById(Long ownerId, Long hotelId, Long roomId);
    RoomDTO updateRoomById(Long ownerId, Long hotelId, Long roomId, RoomDTO roomDTO);
    void deleteRoomById(Long ownerId, Long hotelId, Long roomId);
}
