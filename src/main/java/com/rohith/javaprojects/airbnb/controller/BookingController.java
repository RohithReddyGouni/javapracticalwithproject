package com.rohith.javaprojects.airbnb.controller;

import com.rohith.javaprojects.airbnb.dto.BookingDTO;
import com.rohith.javaprojects.airbnb.dto.BookingRequestDTO;
import com.rohith.javaprojects.airbnb.dto.StripeCheckoutResult;
import com.rohith.javaprojects.airbnb.service.BookingService;
import com.rohith.javaprojects.airbnb.service.CheckoutService;
import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bookings")
public class BookingController {


    private final BookingService bookingService;
    private final CheckoutService checkoutService;

    public BookingController(BookingService bookingService,  CheckoutService checkoutService) {
        this.bookingService = bookingService;
        this.checkoutService = checkoutService;
    }

    @PostMapping
   public ResponseEntity<BookingDTO> createReservation(
           @AuthenticationPrincipal Jwt jwt,
           @RequestHeader("Idempotency-Key") String idempotencyKey,
           @Valid @RequestBody BookingRequestDTO request) {
        Long userId = Long.valueOf(jwt.getSubject());
        BookingDTO booking = bookingService.createReservation(userId,idempotencyKey, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
   }

   @GetMapping("/{bookingId}")
   public ResponseEntity<BookingDTO> getBookingById(
           @AuthenticationPrincipal Jwt jwt,
           @PathVariable Long bookingId) {
        Long userId = Long.valueOf(jwt.getSubject());
        return ResponseEntity.ok(
                bookingService.getBookingById(userId, bookingId));
   }

   @PatchMapping("/{bookingId}/cancel")
   public ResponseEntity<BookingDTO> cancelReservation(
           @AuthenticationPrincipal Jwt jwt,
           @PathVariable Long bookingId) {
        Long userId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok(
                bookingService.cancelReservation(userId, bookingId));
   }

    @PostMapping("/{bookingId}/checkout")
    public ResponseEntity<StripeCheckoutResult> checkout(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long bookingId) throws StripeException {

        Long userId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok(
                checkoutService.checkout(userId, bookingId));
    }

}
