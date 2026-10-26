package com.rohith.javaprojects.airbnb.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public class BookingRequestDTO {

    @NotNull(message = "Room ID is required")
    @Positive(message = "Room ID must be positive")
    private Long roomId;

    @NotNull(message = "check-in date is required")
    private LocalDate checkInDate;

    @NotNull(message = "Check-out date is required")
    private LocalDate checkOutDate;

    @NotNull(message = "Room count is required")
    @Positive(message = "Room count must be greater than zero")
    private Integer roomsCount;

    @NotNull(message = "Guests per room is required")
    @Positive(message = "Guests per room must be greater than zero")
    private Integer guestsPerRoom;

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public Integer getRoomsCount() {
        return roomsCount;
    }

    public void setRoomsCount(Integer roomsCount) {
        this.roomsCount = roomsCount;
    }

    public Integer getGuestsPerRoom() {
        return guestsPerRoom;
    }

    public void setGuestsPerRoom(Integer guestsPerRoom) {
        this.guestsPerRoom = guestsPerRoom;
    }
}
