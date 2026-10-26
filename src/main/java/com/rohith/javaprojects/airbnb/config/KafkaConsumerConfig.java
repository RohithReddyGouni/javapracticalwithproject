package com.rohith.javaprojects.airbnb.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

@Configuration
public class KafkaConsumerConfig {

    private static final Logger log =
            LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Bean
    public CommonErrorHandler kafkaErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate) {

        var recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(
                        record.topic() + ".DLT",
                        record.partition()));

        recoverer.setFailIfSendResultIsError(true);
        recoverer.setWaitForSendResultTimeout(
                Duration.ofSeconds(35));

        var handler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(1000L, 2L));

        handler.addNotRetryableExceptions(
                IllegalArgumentException.class);

        handler.setRetryListeners(
                (record, exception, deliveryAttempt) ->
                        log.warn(
                                "Kafka delivery failed: topic={} partition={} "
                                        + "offset={} attempt={}",
                                record.topic(),
                                record.partition(),
                                record.offset(),
                                deliveryAttempt));

        return handler;
    }
}