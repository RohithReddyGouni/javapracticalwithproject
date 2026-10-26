package com.rohith.javaprojects.airbnb.exception;

public class InvalidWebhookEventException extends RuntimeException {
    public InvalidWebhookEventException(String message) {
        super(message);
    }
}
