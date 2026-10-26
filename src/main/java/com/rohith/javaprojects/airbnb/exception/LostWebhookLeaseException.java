package com.rohith.javaprojects.airbnb.exception;

public class LostWebhookLeaseException extends RuntimeException {

    public LostWebhookLeaseException() {
        super("Webhook processing lease is no longer current");
    }
}