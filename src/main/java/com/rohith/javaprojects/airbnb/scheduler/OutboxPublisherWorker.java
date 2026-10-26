package com.rohith.javaprojects.airbnb.scheduler;

import com.rohith.javaprojects.airbnb.messaging.OutboxKafkaGateway;
import com.rohith.javaprojects.airbnb.service.OutboxClaimService;
import com.rohith.javaprojects.airbnb.service.OutboxProgressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.outbox.publisher-enabled",
        havingValue = "true")
public class OutboxPublisherWorker {

    private static final Logger log =
            LoggerFactory.getLogger(OutboxPublisherWorker.class);

    private final OutboxClaimService claimService;
    private final OutboxKafkaGateway gateway;
    private final OutboxProgressService progressService;

    public OutboxPublisherWorker(
            OutboxClaimService claimService,
            OutboxKafkaGateway gateway,
            OutboxProgressService progressService) {

        this.claimService = claimService;
        this.gateway = gateway;
        this.progressService = progressService;
    }

    @Scheduled(
            scheduler = "outboxScheduler",
            fixedDelayString = "${app.outbox.publisher-delay-ms:1000}")
    public void publishNext() {


        var next = claimService.claimNext();

        if (next.isEmpty()) {
            return;
        }

        var claim = next.get();

        try {
            gateway.publish(claim);

            boolean recorded = progressService.markPublished(claim);

            if (!recorded) {
                log.warn(
                        "Outbox event {} acknowledged by Kafka "
                                + "but claim is no longer current",
                        claim.eventId());
            }

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            log.warn(
                    "Outbox event {} interrupted; claim will expire",
                    claim.eventId());

        } catch (Exception exception) {
            log.error(
                    "Outbox event {} attempt failed after {}",
                    claim.eventId(),
                    exception.getClass().getSimpleName());

            try {
                progressService.scheduleRetry(claim);
            } catch (RuntimeException recoveryException) {
                log.error(
                        "Could not schedule retry for outbox event {} "
                                + "after {}; claim will expire",
                        claim.eventId(),
                        recoveryException.getClass().getSimpleName());
            }
        }
    }
}