package com.rohith.javaprojects.airbnb.scheduler;

import com.rohith.javaprojects.airbnb.payment.StripeRefundGateway;
import com.rohith.javaprojects.airbnb.service.RefundJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.stripe.refund-worker-enabled",
        havingValue = "true")
public class StripeRefundWorker {

    private static final Logger log =
            LoggerFactory.getLogger(StripeRefundWorker.class);

    private final RefundJobService jobService;
    private final StripeRefundGateway gateway;

    public StripeRefundWorker(
            RefundJobService jobService,
            StripeRefundGateway gateway) {

        this.jobService = jobService;
        this.gateway = gateway;
    }

    @Scheduled(
            scheduler = "refundScheduler",
            fixedDelayString = "${app.stripe.refund-worker-delay-ms:5000}")
    public void processNext() {

        var next = jobService.claimNext();

        if (next.isEmpty()) {
            return;
        }

        var claim = next.get();

        try {
            var result = gateway.createOrRetrieve(claim.details());
            jobService.recordResult(claim, result);

        } catch (Exception exception) {
            log.error(
                    "Refund job {} requires investigation after {}",
                    claim.id(),
                    exception.getClass().getSimpleName());

            jobService.recordUncertainOutcome(claim);
        }
    }
}