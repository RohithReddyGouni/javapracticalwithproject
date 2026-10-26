package com.rohith.javaprojects.airbnb.messaging;

import com.rohith.javaprojects.airbnb.dto.OutboxClaim;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class OutboxKafkaGateway {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final String bookingTopic;

    public OutboxKafkaGateway(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.kafka.booking-topic}") String bookingTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.bookingTopic = bookingTopic;
    }

    @Transactional
    public void publish(OutboxClaim claim)
            throws InterruptedException,
            ExecutionException,
            TimeoutException
            {
        kafkaTemplate.send(
                bookingTopic,
                claim.messageKey(),
                claim.payload()
        ).get(35, TimeUnit.SECONDS);

    }
}
