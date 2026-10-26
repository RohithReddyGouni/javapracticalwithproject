package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.dto.RoomDTO;
import com.rohith.javaprojects.airbnb.entity.Hotel;
import com.rohith.javaprojects.airbnb.entity.Room;
import com.rohith.javaprojects.airbnb.exception.ResourceConflictException;
import com.rohith.javaprojects.airbnb.exception.ResourceNotFoundException;
import com.rohith.javaprojects.airbnb.repository.HotelRepository;
import com.rohith.javaprojects.airbnb.repository.InventoryRepository;
import com.rohith.javaprojects.airbnb.repository.RoomRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
public class RoomServiceImpl implements RoomService {

    private final HotelRepository hotelRepository;

    private final RoomRepository roomRepository;

    private final InventoryRepository inventoryRepository;

    public RoomServiceImpl(
            HotelRepository hotelRepository,
            RoomRepository roomRepository,
            InventoryRepository inventoryRepository) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public RoomDTO createRoom(Long ownerId, Long hotelId, RoomDTO roomDTO) {
        Hotel hotel = getOwnedHotel(ownerId, hotelId);
        Room room = new Room();
        room.setType(roomDTO.getType());
        room.setBasePrice(roomDTO.getBasePrice());
        room.setTotalCount(roomDTO.getTotalCount());
        room.setCapacity(roomDTO.getCapacity());
        room.setHotel(hotel);

        Room savedRoom = roomRepository.save(room);
        return toDto(savedRoom);
    }

    @Override
    public List<RoomDTO> getRoomsByHotelId(Long ownerId, Long hotelId) {

        getOwnedHotel(ownerId, hotelId);
        List<Room> rooms = roomRepository.findByHotel_Id(hotelId);
        List<RoomDTO> response = new ArrayList<>();

        for(Room room: rooms) {
            response.add(toDto(room));
        }
        return response;
    }


    @Override
    public RoomDTO getRoomById(Long ownerId, Long hotelId, Long roomId) {
        getOwnedHotel(ownerId, hotelId);
        Room room = roomRepository.findByIdAndHotel_Id(roomId, hotelId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Room not found with ID: " + roomId
                                + " in hotel: " + hotelId));

       return toDto(room);

    }

    @Override
    @Transactional
    public RoomDTO updateRoomById(Long ownerId, Long hotelId, Long roomId, RoomDTO roomDTO) {
        getOwnedHotel(ownerId, hotelId);
        Room room = roomRepository.findForUpdate(roomId, hotelId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Room not found with ID:: "+ roomId
                            + " in hotel:: "+hotelId
                ));
        boolean totalCountChanged =
                !room.getTotalCount().equals(roomDTO.getTotalCount());
        if(totalCountChanged && inventoryRepository.existsByRoom_Id(roomId)) {
            throw new ResourceConflictException(
                    "Room total count cannot change after inventory has been initialized"
            );
        }

        room.setType(roomDTO.getType());
        room.setBasePrice(roomDTO.getBasePrice());
        room.setTotalCount(roomDTO.getTotalCount());
        room.setCapacity(roomDTO.getCapacity());
        Room savedRoom = roomRepository.save(room);
        return toDto(savedRoom);

    }

    @Override
    @Transactional
    public void deleteRoomById(
            Long ownerId, Long hotelId, Long roomId) {
        getOwnedHotel(ownerId, hotelId);
        Room room = roomRepository.findForUpdate(roomId, hotelId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Room not found with the ID :: "+roomId
                        + "  in hotel ID :: "+hotelId
                ));
        if (inventoryRepository.existsByRoom_Id(roomId)) {
            throw new ResourceConflictException(
                    "Room category cannot be deleted because inventory exists");
        }
        roomRepository.delete(room);
    }



    private RoomDTO toDto(Room room) {
        RoomDTO roomDTO = new RoomDTO();
        roomDTO.setId(room.getId());
        roomDTO.setType(room.getType());
        roomDTO.setBasePrice(room.getBasePrice());
        roomDTO.setTotalCount(room.getTotalCount());
        roomDTO.setCapacity(room.getCapacity());

        return roomDTO;

    }

    private Hotel getOwnedHotel(Long ownerId, Long hotelId) {
        return hotelRepository.findByIdAndOwner_Id(hotelId, ownerId)
                .orElseThrow(() ->new ResourceNotFoundException(
                      "Hotel not found with ID:: "+ hotelId
                ));
    }

}
