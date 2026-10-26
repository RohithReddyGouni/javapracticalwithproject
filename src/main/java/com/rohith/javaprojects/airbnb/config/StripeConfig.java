package com.rohith.javaprojects.airbnb.config;

import com.stripe.StripeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    @Bean
    public StripeClient stripeClient(
            @Value("${app.stripe.secret-key}") String secretKey) {
        if (!secretKey.startsWith("sk_test_")) {
            throw new IllegalArgumentException(
                    "This project currently requires a Stripe test secret key");
        }
        return StripeClient.builder()
                .setApiKey(secretKey)
                .setConnectTimeout(5000)
                .setReadTimeout(20000)
                .setMaxNetworkRetries(1)
                .build();
    }
}
