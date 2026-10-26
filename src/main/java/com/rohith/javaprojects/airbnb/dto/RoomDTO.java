package com.rohith.javaprojects.airbnb.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class RoomDTO {

    private Long id;

    @NotBlank(message = "Room type is required")
    private String type;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.01", message = "Base price must be at least 0.01")
    @Digits(integer = 8, fraction = 2, message = "Base price must have at most 8 integer digits and 2 decimal places")
    private BigDecimal basePrice;

    @NotNull(message = "Total count is required")
    @Positive(message = "Total count must be greater than zero")
    private Integer totalCount;

    @NotNull(message = "Capacity is required")
    @Positive(message = "Capacity must be greater than zero")
    private Integer capacity;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }
}
