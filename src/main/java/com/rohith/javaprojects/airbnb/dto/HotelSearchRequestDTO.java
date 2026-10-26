package com.rohith.javaprojects.airbnb.dto;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class HotelSearchRequestDTO {

    @NotBlank(message = "City is required")
    @Size(max = 255)
    private String city;

    @NotNull(message = "Check-In date is required")
    @FutureOrPresent(message = "Check-In date cannot be in the past")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkInDate;

    @NotNull(message = "Check-out date is required")
    @Future(message = "Check-out date must be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkoutDate;

    @NotNull(message = "Room count is required")
    @Positive(message = "Room count must be greater than zero")
    private Integer roomsCount;

    @NotNull(message = "Guests per room is required")
    @Positive(message = "Guests per room must be greater than zero")
    private Integer guestsPerRoom;

    @NotNull(message = "Page is required")
    @Min(value = 0, message = "Page cannot be negative")
    private Integer page = 0;

    @NotNull(message = "Size is required")
    @Min(value = 1, message = "Size must be at least 1")
    @Max(value = 50, message = "Size must not exceed 50")
    private Integer size = 10;


    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDate getCheckoutDate() {
        return checkoutDate;
    }

    public void setCheckoutDate(LocalDate checkoutDate) {
        this.checkoutDate = checkoutDate;
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

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
