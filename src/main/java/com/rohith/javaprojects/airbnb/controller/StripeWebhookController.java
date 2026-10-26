package com.rohith.javaprojects.airbnb.controller;

import com.google.gson.JsonParseException;
import com.rohith.javaprojects.airbnb.service.StripeWebhookIngestionService;
import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/webhooks/stripe")
public class StripeWebhookController {

    private final StripeClient stripeClient;
    private final StripeWebhookIngestionService ingestionService;
    private final String webhookSecret;

    public StripeWebhookController(
            StripeClient stripeClient,
            StripeWebhookIngestionService ingestionService,
            @Value("${app.stripe.webhook-secret}") String webhookSecret) {

        this.stripeClient = stripeClient;
        this.ingestionService = ingestionService;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<Void> receive(
            @RequestBody byte[] body,
            @RequestHeader("Stripe-Signature") String signature) {

        if (body.length > 1_048_576) {
            return ResponseEntity.status(413).build();
        }

        String payload = new String(body, StandardCharsets.UTF_8);

        Event event;

        try {
            event = stripeClient.constructEvent(
                    payload, signature, webhookSecret);
        } catch (SignatureVerificationException | JsonParseException exception) {
            return ResponseEntity.badRequest().build();
        }

        // This learning project currently accepts test events only.
        if (Boolean.TRUE.equals(event.getLivemode())) {
            return ResponseEntity.badRequest().build();
        }

        ingestionService.storeVerifiedEvent(
                event.getId(),
                event.getType(),
                payload);

        return ResponseEntity.ok().build();
    }
}