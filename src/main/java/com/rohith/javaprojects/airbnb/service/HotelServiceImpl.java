package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.dto.HotelDTO;
import com.rohith.javaprojects.airbnb.entity.Hotel;
import com.rohith.javaprojects.airbnb.entity.User;
import com.rohith.javaprojects.airbnb.exception.ResourceConflictException;
import com.rohith.javaprojects.airbnb.exception.ResourceNotFoundException;
import com.rohith.javaprojects.airbnb.repository.HotelRepository;
import com.rohith.javaprojects.airbnb.repository.RoomRepository;
import com.rohith.javaprojects.airbnb.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class HotelServiceImpl implements HotelService{
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public HotelServiceImpl(HotelRepository hotelRepository,
                            RoomRepository roomRepository,
                            UserRepository userRepository ) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    @Override
    public HotelDTO createHotel(Long ownerId,HotelDTO hotelDTO) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID:: "+ ownerId));
        Hotel hotel = new Hotel();
        hotel.setName(hotelDTO.getName());
        hotel.setCity(hotelDTO.getCity());
        hotel.setActive(false);
        hotel.setOwner(owner);
        Hotel savedHotel = hotelRepository.save(hotel);
        return toDto(savedHotel);
    }

    @Override
    @Cacheable(
            cacheNames = "hotelByOwner",
            key = "'owner:'+ #p0 +':hotel:'+ #p1"
    )
    public HotelDTO getHotelById(Long ownerId, Long hotelId) {
        Hotel hotel = hotelRepository.findByIdAndOwner_Id(hotelId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Hotel not found with ID:: "+ hotelId
                ));

        return toDto(hotel);
    }

    @Override
    public List<HotelDTO> getAllHotels(Long ownerId) {
        List<Hotel> hotels = hotelRepository.findByOwner_Id(ownerId);
        List<HotelDTO> response = new ArrayList<>();
        for(Hotel hotel: hotels) {
            response.add(toDto(hotel));
        }
        return response;
    }

    @Override
    @CacheEvict(
            cacheNames = "hotelByOwner",
            key = "'owner:' + #p0 + ':hotel:' + #p1"
    )
    public HotelDTO updateHotelById(Long ownerId, Long hotelId, HotelDTO hotelDTO) {
        Hotel existingHotel = hotelRepository.findByIdAndOwner_Id(hotelId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Hotel not found with the ID:: "+hotelId
                ));
        existingHotel.setCity(hotelDTO.getCity());
        existingHotel.setName(hotelDTO.getName());

        Hotel updatedHotel = hotelRepository.save(existingHotel);
        return toDto(updatedHotel);
    }

    @Override
    @CacheEvict(
            cacheNames = "hotelByOwner",
            key = "'owner:' + #p0 + ':hotel:' + #p1"
    )
    public void deleteHotelById(Long ownerId, Long hotelId) {
        Hotel hotel = hotelRepository.findByIdAndOwner_Id(hotelId,ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Hotel not found with the ID:: "+ hotelId
                ));
        if (roomRepository.existsByHotel_Id(hotelId)) {
            throw new ResourceConflictException(
                    "Delete the hotel's room categories before deleting the hotel"
            );
        }
        hotelRepository.delete(hotel);
    }

    @Override
    @CacheEvict(
            cacheNames = "hotelByOwner",
            key = "'owner:' + #p0 + ':hotel:' + #p1"
    )
    public void activateHotel(Long ownerId, Long hotelId) {
        Hotel hotel = hotelRepository.findByIdAndOwner_Id(hotelId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Hotel not found with the ID:: "+hotelId
                ));
        hotel.setActive(true);
        hotelRepository.save(hotel);
    }

    private HotelDTO toDto(Hotel hotel) {
        HotelDTO hotelDTO = new HotelDTO();
        hotelDTO.setId(hotel.getId());
        hotelDTO.setName(hotel.getName());
        hotelDTO.setCity(hotel.getCity());
        hotelDTO.setActive(hotel.isActive());
        return hotelDTO;

    }


}
