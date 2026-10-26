package com.rohith.javaprojects.airbnb.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentReturnController {

    @GetMapping("/success")
    public Map<String, String> success() {
        return Map.of(
                "message",
                "Checkout completed. Booking confirmation is pending payment verification.");
    }

    @GetMapping("/cancel")
    public Map<String, String> cancel() {
        return Map.of(
                "message",
                "You left checkout. This does not cancel your reservation.");
    }
}
