package com.rohith.javaprojects.airbnb.payment;

import com.rohith.javaprojects.airbnb.dto.RefundDetails;
import com.rohith.javaprojects.airbnb.dto.StripeRefundResult;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.RefundCreateParams;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StripeRefundGateway {

    private final StripeClient stripeClient;

    public StripeRefundGateway(StripeClient stripeClient) {
        this.stripeClient = stripeClient;
    }

    @Transactional(propagation = Propagation.NEVER)
    public StripeRefundResult createOrRetrieve(
            RefundDetails details) throws StripeException {

        var refunds = stripeClient.v1().refunds();

        if (details.stripeRefundId() != null) {
            return toResult(
                    refunds.retrieve(details.stripeRefundId()));
        }

        var params = RefundCreateParams.builder()
                .setPaymentIntent(details.paymentIntentId())
                .setAmount(details.amountMinor())
                .putMetadata(
                        "refundKey",
                        details.refundKey().toString())
                .build();

        var options = RequestOptions.builder()
                .setIdempotencyKey("refund:" + details.refundKey())
                .build();

        Refund refund = refunds.create(params, options);

        return toResult(refund);
    }

    private StripeRefundResult toResult(Refund refund) {
        String refundKey = refund.getMetadata() == null
                ? null
                : refund.getMetadata().get("refundKey");

        return new StripeRefundResult(
                refund.getId(),
                refund.getStatus(),
                refund.getAmount(),
                refund.getCurrency(),
                refund.getPaymentIntent(),
                refundKey);
    }
}