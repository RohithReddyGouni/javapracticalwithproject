package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.repository.StripeWebhookEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StripeWebhookIngestionService {

    private final StripeWebhookEventRepository repository;

    public StripeWebhookIngestionService(
            StripeWebhookEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void storeVerifiedEvent(
            String eventId,
            String eventType,
            String payload) {

        repository.insertIfAbsent(eventId, eventType, payload);
    }
}