package com.rohith.javaprojects.airbnb.messaging;

import com.rohith.javaprojects.airbnb.event.BookingConfirmedEvent;
import com.rohith.javaprojects.airbnb.service.BookingConfirmationConsumerService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
@ConditionalOnProperty(
        name = "app.kafka.booking-consumer-enabled",
        havingValue = "true")
public class BookingConfirmationListener {

    private static final Logger log =
            LoggerFactory.getLogger(
                    BookingConfirmationListener.class);

    private final JsonMapper jsonMapper;
    private final BookingConfirmationConsumerService service;

    public BookingConfirmationListener(
            JsonMapper jsonMapper,
            BookingConfirmationConsumerService service) {

        this.jsonMapper = jsonMapper;
        this.service = service;
    }

    @KafkaListener(
            id = "booking-confirmation-report",
            topics = "${app.kafka.booking-topic}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void consume(ConsumerRecord<String, String> record) {

        if (record.value() == null) {
            throw new IllegalArgumentException(
                    "Booking event payload is required");
        }

        BookingConfirmedEvent event;

        try {
            event = jsonMapper.readValue(
                    record.value(),
                    BookingConfirmedEvent.class);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(
                    "Invalid booking event JSON");
        }

        if (event == null || event.bookingId() == null) {
            throw new IllegalArgumentException(
                    "Booking ID is required");
        }

        String expectedKey = "BOOKING:" + event.bookingId();

        if (!expectedKey.equals(record.key())) {
            throw new IllegalArgumentException(
                    "Kafka key does not match booking ID");
        }

        boolean processed = service.process(event);

        log.info(
                "Booking event={} booking={} partition={} offset={} "
                        + "thread={} processed={}",
                event.eventId(),
                event.bookingId(),
                record.partition(),
                record.offset(),
                Thread.currentThread().getName(),
                processed);
    }
}