package com.rohith.javaprojects.airbnb.repository.projection;

import java.math.BigDecimal;

public interface HotelSearchProjection {
    Long getHotelId();
    String getHotelName();
    String getCity();

    Long getRoomId();
    String getRoomType();
    Integer getCapacity();

    BigDecimal getTotalPrice();
}
