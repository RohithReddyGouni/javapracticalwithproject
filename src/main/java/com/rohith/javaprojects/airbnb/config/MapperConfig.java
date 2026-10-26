package com.rohith.javaprojects.airbnb.config;

import com.rohith.javaprojects.airbnb.dto.BookingDTO;
import com.rohith.javaprojects.airbnb.entity.Booking;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.spi.MatchingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();

        mapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT);

        mapper.typeMap(Booking.class, BookingDTO.class)
                .addMappings(mapping -> {
                    mapping.map(
                            source -> source.getRoom().getHotel().getId(),
                            BookingDTO::setHotelId);

                    mapping.map(
                            source -> source.getRoom().getId(),
                            BookingDTO::setRoomId);

                    mapping.map(
                            source -> source.getRoom().getType(),
                            BookingDTO::setRoomType);
                });
        return mapper;
    }
}
