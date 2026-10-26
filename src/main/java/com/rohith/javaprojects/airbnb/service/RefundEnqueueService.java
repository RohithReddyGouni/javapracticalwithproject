package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.entity.PaymentAttempt;
import com.rohith.javaprojects.airbnb.entity.enums.PaymentStatus;
import com.rohith.javaprojects.airbnb.repository.RefundRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RefundEnqueueService {

    private final RefundRequestRepository repository;

    public RefundEnqueueService(RefundRequestRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueFullRefund(PaymentAttempt attempt) {
        if (attempt.getId() == null
                || attempt.getStatus() != PaymentStatus.REFUND_PENDING) {
            throw new IllegalStateException(
                    "A saved refund-pending payment attempt is required");
        }

        repository.insertIfAbsent(
                UUID.randomUUID(),
                attempt.getId(),
                attempt.getExpectedAmountMinor(),
                attempt.getCurrency());
    }
}