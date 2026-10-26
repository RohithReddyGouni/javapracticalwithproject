package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.entity.Booking;
import com.rohith.javaprojects.airbnb.entity.OutboxEvent;
import com.rohith.javaprojects.airbnb.entity.enums.BookingStatus;
import com.rohith.javaprojects.airbnb.entity.enums.OutboxStatus;
import com.rohith.javaprojects.airbnb.event.BookingConfirmedEvent;
import com.rohith.javaprojects.airbnb.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

@Service
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;

    private final JsonMapper jsonMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, JsonMapper jsonMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordBookingConfirmed(Booking booking) {

        if (booking.getId() == null
                || booking.getStatus() != BookingStatus.CONFIRMED ) {
            throw new IllegalArgumentException(
                    " A saved, confirmed booking is required");
        }

        UUID eventId = UUID.randomUUID();

        Instant now = Instant.now();

        BookingConfirmedEvent payload = new BookingConfirmedEvent(
                eventId,
                "BookingConfirmed",
                1,
                now,
                booking.getId(),
                booking.getUser().getId(),
                booking.getRoom().getId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getRoomsCount(),
                booking.getTotalPrice(),
                booking.getCurrency());

        OutboxEvent event = new OutboxEvent();
        event.setId(eventId);
        event.setEventKey(
                "booking-confirmed:"+booking.getId()+":v1");
        event.setAggregateType("BOOKING");
        event.setAggregateId(booking.getId());
        event.setEventType("BookingConfirmed");
        event.setPayload(jsonMapper.writeValueAsString(payload));
        event.setStatus(OutboxStatus.PENDING);
        event.setAttempts(0);
        event.setNextAttemptAt(now);
        event.setCreatedAt(now);

        outboxEventRepository.save(event);
    }
}
