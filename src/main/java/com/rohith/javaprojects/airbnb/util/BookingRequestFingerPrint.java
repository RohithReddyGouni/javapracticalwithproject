package com.rohith.javaprojects.airbnb.util;


import com.rohith.javaprojects.airbnb.dto.BookingRequestDTO;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class BookingRequestFingerPrint {

    public String calculate(BookingRequestDTO request) {
        String canonicalRequest = String.join("|",
                "booking-request-v1",
                request.getRoomId().toString(),
                request.getCheckInDate().toString(),
                request.getCheckOutDate().toString(),
                request.getRoomsCount().toString(),
                request.getGuestsPerRoom().toString());

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    canonicalRequest.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable", exception);
        }
    }
}
