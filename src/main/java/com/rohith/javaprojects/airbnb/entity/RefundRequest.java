package com.rohith.javaprojects.airbnb.entity;

import com.rohith.javaprojects.airbnb.entity.enums.RefundStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refund_requests")
public class RefundRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID refundKey;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "payment_attempt_id",
            nullable = false,
            unique = true)
    private PaymentAttempt paymentAttempt;

    @Column(nullable = false, updatable = false)
    private Long amountMinor;

    @Column(nullable = false, length = 3, updatable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RefundStatus status;

    @Column(unique = true)
    private String stripeRefundId;

    @Column(nullable = false)
    private Integer attempts;

    @Column(nullable = false)
    private Instant nextAttemptAt;

    private Instant leaseUntil;

    private UUID leaseToken;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(length = 120)
    private String lastErrorCode;

    // Generate getters and setters.

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getRefundKey() {
        return refundKey;
    }

    public void setRefundKey(UUID refundKey) {
        this.refundKey = refundKey;
    }

    public PaymentAttempt getPaymentAttempt() {
        return paymentAttempt;
    }

    public void setPaymentAttempt(PaymentAttempt paymentAttempt) {
        this.paymentAttempt = paymentAttempt;
    }

    public Long getAmountMinor() {
        return amountMinor;
    }

    public void setAmountMinor(Long amountMinor) {
        this.amountMinor = amountMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public void setStatus(RefundStatus status) {
        this.status = status;
    }

    public String getStripeRefundId() {
        return stripeRefundId;
    }

    public void setStripeRefundId(String stripeRefundId) {
        this.stripeRefundId = stripeRefundId;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(Instant nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public Instant getLeaseUntil() {
        return leaseUntil;
    }

    public void setLeaseUntil(Instant leaseUntil) {
        this.leaseUntil = leaseUntil;
    }

    public UUID getLeaseToken() {
        return leaseToken;
    }

    public void setLeaseToken(UUID leaseToken) {
        this.leaseToken = leaseToken;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public void setLastErrorCode(String lastErrorCode) {
        this.lastErrorCode = lastErrorCode;
    }
}