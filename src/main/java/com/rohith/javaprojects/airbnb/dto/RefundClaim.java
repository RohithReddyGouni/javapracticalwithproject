package com.rohith.javaprojects.airbnb.dto;

import java.util.UUID;

public record RefundClaim(
        Long id,
        UUID leaseToken,
        RefundDetails details
) {
}