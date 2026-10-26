package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.dto.*;
import com.rohith.javaprojects.airbnb.entity.*;
import com.rohith.javaprojects.airbnb.entity.enums.*;
import com.rohith.javaprojects.airbnb.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefundJobService {

    private final RefundRequestRepository refundRepository;
    private final PaymentAttemptRepository paymentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public RefundJobService(
            RefundRequestRepository refundRepository,
            PaymentAttemptRepository paymentRepository) {

        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public Optional<RefundClaim> claimNext() {
        Instant now = Instant.now();

        refundRepository.recoverExpiredLeases(now);

        var jobs = refundRepository.findNextPending(now);

        if (jobs.isEmpty()) {
            return Optional.empty();
        }

        RefundRequest job = jobs.get(0);
        PaymentAttempt payment = job.getPaymentAttempt();

        boolean valid =
                payment.getStatus() == PaymentStatus.REFUND_PENDING
                && payment.getStripePaymentIntentId() != null
                && !payment.getStripePaymentIntentId().isBlank()
                && job.getAmountMinor()
                        .equals(payment.getExpectedAmountMinor())
                && job.getCurrency()
                        .equalsIgnoreCase(payment.getCurrency());

        if (!valid || job.getAttempts() > 0) {
            review(job, "REFUND_JOB_REQUIRES_REVIEW");
            return Optional.empty();
        }

        UUID token = UUID.randomUUID();

        job.setStatus(RefundStatus.PROCESSING);
        job.setLeaseToken(token);
        job.setLeaseUntil(now.plusSeconds(120));
        job.setAttempts(job.getAttempts() + 1);
        job.setUpdatedAt(now);

        return Optional.of(new RefundClaim(
                job.getId(),
                token,
                new RefundDetails(
                        job.getRefundKey(),
                        payment.getStripePaymentIntentId(),
                        job.getAmountMinor(),
                        job.getStripeRefundId())));
    }

    @Transactional
    public void recordResult(
            RefundClaim claim, StripeRefundResult result) {

        RefundRequest initial = refundRepository
                .findById(claim.id()).orElseThrow();

        // Lock payment before refund to maintain consistent ordering.
        PaymentAttempt payment = paymentRepository
                .findByIdForUpdate(initial.getPaymentAttempt().getId())
                .orElseThrow();

        entityManager.refresh(payment);

        RefundRequest job = refundRepository
                .findByIdForUpdate(claim.id()).orElseThrow();

        entityManager.refresh(job);

        if (!ownsLease(job, claim)) {
            return;
        }

        boolean matches =
                result.refundId() != null
                && !result.refundId().isBlank()
                && job.getAmountMinor().equals(result.amountMinor())
                && job.getCurrency().equalsIgnoreCase(result.currency())
                && payment.getStripePaymentIntentId()
                        .equals(result.paymentIntentId())
                && job.getRefundKey().toString()
                        .equals(result.refundKey())
                && (job.getStripeRefundId() == null
                    || job.getStripeRefundId().equals(result.refundId()));

        if (!matches) {
            review(job, "REFUND_RESULT_MISMATCH");
            return;
        }

        job.setStripeRefundId(result.refundId());

        if (payment.getStatus() != PaymentStatus.REFUND_PENDING) {
            review(job, "PAYMENT_STATE_REQUIRES_REVIEW");
            return;
        }

        if ("succeeded".equals(result.status())) {
            job.setStatus(RefundStatus.SUCCEEDED);
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setUpdatedAt(Instant.now());
            job.setLastErrorCode(null);

        } else if ("failed".equals(result.status())
                || "canceled".equals(result.status())) {
            job.setStatus(RefundStatus.FAILED);
            job.setLastErrorCode("STRIPE_REFUND_NOT_COMPLETED");

        } else {
            review(job, "REFUND_NOT_YET_CONFIRMED");
            return;
        }

        clearLease(job);
    }

    @Transactional
    public void recordUncertainOutcome(RefundClaim claim) {
        RefundRequest job = refundRepository
                .findByIdForUpdate(claim.id()).orElseThrow();

        entityManager.refresh(job);

        if (ownsLease(job, claim)) {
            review(job, "REFUND_OUTCOME_UNCERTAIN");
        }
    }

    private boolean ownsLease(RefundRequest job, RefundClaim claim) {
        return job.getStatus() == RefundStatus.PROCESSING
                && claim.leaseToken().equals(job.getLeaseToken())
                && job.getLeaseUntil() != null
                && job.getLeaseUntil().isAfter(Instant.now());
    }

    private void review(RefundRequest job, String code) {
        job.setStatus(RefundStatus.RECONCILIATION_REQUIRED);
        job.setLastErrorCode(code);
        clearLease(job);
    }

    private void clearLease(RefundRequest job) {
        job.setLeaseToken(null);
        job.setLeaseUntil(null);
        job.setUpdatedAt(Instant.now());
    }
}