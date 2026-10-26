package com.rohith.javaprojects.airbnb.payment;


import com.rohith.javaprojects.airbnb.dto.CheckoutDetails;
import com.rohith.javaprojects.airbnb.dto.StripeCheckoutResult;
import com.rohith.javaprojects.airbnb.dto.StripeSessionSnapshot;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;

@Component
public class StripeCheckoutGateway {

    private final StripeClient stripeClient;

    private final String successUrl;

    private final String cancelUrl;


    public StripeCheckoutGateway(StripeClient stripeClient,
                                 @Value("${app.stripe.success-url}")String successUrl,
                                 @Value("${app.stripe.cancel-url}")String cancelUrl) {
        this.stripeClient = stripeClient;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
    }

    @Transactional(propagation = Propagation.NEVER)
    public StripeCheckoutResult createCheckout(
            CheckoutDetails details) throws StripeException {
        var product = SessionCreateParams.LineItem.PriceData
                .ProductData.builder()
                .setName("Hotel reservation "+details.bookingId())
                .build();

        var price = SessionCreateParams.LineItem.PriceData.builder()
                .setCurrency(details.currency().toLowerCase(Locale.ROOT))
                .setUnitAmount(details.amountMinor())
                .setProductData(product)
                .build();
        var item = SessionCreateParams.LineItem.builder()
                .setQuantity(1L)
                .setPriceData(price)
                .build();

        var params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .addAllowedPaymentMethodType(
                        SessionCreateParams.AllowedPaymentMethodType.CARD)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setClientReferenceId(details.bookingId().toString())
                .putMetadata("bookingId", details.bookingId().toString())
                .putMetadata("attemptKey", details.attemptKey().toString())
                .addLineItem(item)
                .build();

        var options = RequestOptions.builder()
                .setIdempotencyKey("checkout:" + details.attemptKey())
                .build();

        var session = stripeClient.v1().checkout().sessions()
                .create(params, options);

        return new StripeCheckoutResult(
                session.getId(),
                session.getUrl());
    }

    @Transactional(propagation = Propagation.NEVER)
    public StripeSessionSnapshot retrieveSession(
            String sessionId) throws StripeException {

        var session = stripeClient.v1().checkout().sessions()
                .retrieve(sessionId);

        return toSnapshot(session);
    }

    @Transactional(propagation = Propagation.NEVER)
    public StripeSessionSnapshot expireIfOpen(
            String sessionId) throws StripeException {

        var sessions = stripeClient.v1().checkout().sessions();
        var session = sessions.retrieve(sessionId);

        if (!"open".equals(session.getStatus())) {
            return toSnapshot(session);
        }

        try {
            return toSnapshot(sessions.expire(sessionId));
        } catch (com.stripe.exception.InvalidRequestException exception) {
            // The session may have changed after our initial retrieval.
            var latest = sessions.retrieve(sessionId);

            if (!"open".equals(latest.getStatus())) {
                return toSnapshot(latest);
            }

            throw exception;
        }
    }
    private StripeSessionSnapshot toSnapshot(
            com.stripe.model.checkout.Session session) {

        return new StripeSessionSnapshot(
                session.getId(),
                session.getUrl(),
                session.getStatus(),
                session.getPaymentStatus(),
                session.getAmountTotal(),
                session.getCurrency(),
                session.getClientReferenceId(),
                session.getMetadata() == null
                        ? Map.of()
                        : Map.copyOf(session.getMetadata()),
                session.getPaymentIntent(),
                session.getLivemode());
    }



}
