package com.rohith.javaprojects.airbnb.scheduler;

import com.rohith.javaprojects.airbnb.dto.WebhookClaim;
import com.rohith.javaprojects.airbnb.entity.enums.WebhookFailureCode;
import com.rohith.javaprojects.airbnb.exception.InvalidWebhookEventException;
import com.rohith.javaprojects.airbnb.exception.LostWebhookLeaseException;
import com.rohith.javaprojects.airbnb.exception.ResourceConflictException;
import com.rohith.javaprojects.airbnb.payment.StripeWebhookSessionLoader;
import com.rohith.javaprojects.airbnb.service.*;
import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.stripe.webhook-worker-enabled",
        havingValue = "true")
public class StripeWebhookWorker {

    private static final Logger log =
            LoggerFactory.getLogger(StripeWebhookWorker.class);

    private final WebhookClaimService claimService;
    private final StripeWebhookSessionLoader sessionLoader;
    private final WebhookPaymentProcessor paymentProcessor;
    private final WebhookProgressService progressService;

    public StripeWebhookWorker(
            WebhookClaimService claimService,
            StripeWebhookSessionLoader sessionLoader,
            WebhookPaymentProcessor paymentProcessor,
            WebhookProgressService progressService) {

        this.claimService = claimService;
        this.sessionLoader = sessionLoader;
        this.paymentProcessor = paymentProcessor;
        this.progressService = progressService;
    }

    @Scheduled(
            scheduler = "webhookScheduler",
            fixedDelayString = "${app.stripe.webhook-worker-delay-ms:5000}")
    public void processBatch() {

        for (WebhookClaim claim : claimService.claimBatch()) {
            try {
                processOne(claim);
            } catch (Exception exception) {
                // Includes failures while recording retry/failure state.
                // The lease allows recovery on a later run.
                log.error(
                        "Webhook worker could not finish event {}: {}",
                        claim.eventId(),
                        exception.getClass().getSimpleName());
            }
        }
    }

    private void processOne(WebhookClaim claim) {
        try {
            var session = sessionLoader.load(claim);

            paymentProcessor.processCheckoutEvent(claim, session);

        } catch (LostWebhookLeaseException exception) {
            log.info(
                    "Skipping event {} because its lease is no longer current",
                    claim.eventId());

        } catch (InvalidWebhookEventException exception) {
            progressService.failPermanently(
                    claim, WebhookFailureCode.INVALID_EVENT);

        } catch (ResourceConflictException exception) {
            progressService.failPermanently(
                    claim, WebhookFailureCode.BUSINESS_STATE_CONFLICT);

        } catch (StripeException exception) {
            Integer status = exception.getStatusCode();

            boolean retryable =
                    exception instanceof ApiConnectionException
                    || status != null
                    && (status == 429 || status >= 500);

            if (retryable) {
                progressService.scheduleRetry(claim);
            } else {
                progressService.failPermanently(
                        claim, WebhookFailureCode.PROVIDER_REQUEST_REJECTED);
            }

        } catch (RuntimeException exception) {
            log.warn(
                    "Retrying event {} after {}",
                    claim.eventId(),
                    exception.getClass().getSimpleName());

            progressService.scheduleRetry(claim);
        }
    }
}