package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.dto.HotelSearchRequestDTO;
import com.rohith.javaprojects.airbnb.dto.HotelSearchResultDTO;
import com.rohith.javaprojects.airbnb.dto.SearchResponseDTO;
import com.rohith.javaprojects.airbnb.exception.InvalidRequestException;
import com.rohith.javaprojects.airbnb.repository.InventoryRepository;

import com.rohith.javaprojects.airbnb.repository.projection.HotelSearchProjection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class HotelSearchServiceImpl implements  HotelSearchService{

    private final InventoryRepository inventoryRepository;

    public HotelSearchServiceImpl(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResponseDTO searchHotels(HotelSearchRequestDTO request) {
        LocalDate checkIn = request.getCheckInDate();
        LocalDate checkOut = request.getCheckoutDate();

        if (checkIn == null || checkOut == null) {
            throw new InvalidRequestException(
                    "Check-In and Check-Out dates are required"
            );
        }

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if(checkIn.isBefore(LocalDate.now()) || nights < 1 || nights > 365) {
            throw new InvalidRequestException(
                    "Stay must start today or later and contain 1-365 nights"
            );
        }

        Pageable pageable = PageRequest.of(
                request.getPage(), request.getSize()
        );

        Slice<HotelSearchProjection> matches =
                inventoryRepository.searchAvailableRooms(
                        request.getCity().strip(),
                        checkIn,
                        checkOut,
                        request.getRoomsCount(),
                        request.getGuestsPerRoom(),
                        nights,
                        pageable
                );

        List<HotelSearchResultDTO> response = new ArrayList<>();
        for(HotelSearchProjection match: matches) {
            HotelSearchResultDTO hotelSearchResultDTO = new HotelSearchResultDTO();
            hotelSearchResultDTO.setHotelId(match.getHotelId());
            hotelSearchResultDTO.setHotelName(match.getHotelName());
            hotelSearchResultDTO.setCapacity(match.getCapacity());
            hotelSearchResultDTO.setRoomId(match.getRoomId());
            hotelSearchResultDTO.setTotalPrice(match.getTotalPrice());
            hotelSearchResultDTO.setRoomType(match.getRoomType());
            hotelSearchResultDTO.setCity(match.getCity());
            response.add(hotelSearchResultDTO);

        }
        return new SearchResponseDTO(
                response,
                matches.getNumber(),
                matches.getSize(),
                matches.hasNext()
        );
    }
}
