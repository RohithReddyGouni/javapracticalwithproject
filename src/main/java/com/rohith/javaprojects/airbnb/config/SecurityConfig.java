package com.rohith.javaprojects.airbnb.config;

import com.rohith.javaprojects.airbnb.security.HotelSearchRateLimitFilter;
import com.rohith.javaprojects.airbnb.service.RedisRateLimitService;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity httpSecurity,
            RedisRateLimitService rateLimitService,
            JsonMapper jsonMapper,
            @Value("${app.rate-limit.hotel-search.limit}") int limit,
            @Value("${app.rate-limit.hotel-search.window-ms}") long windowMillis) throws Exception {

        var rateLimitFilter = new HotelSearchRateLimitFilter(
                rateLimitService,
                jsonMapper,
                limit,
                windowMillis);
        return httpSecurity
                .addFilterBefore( rateLimitFilter,
                        BearerTokenAuthenticationFilter.class)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                                .sessionCreationPolicy(
                                        SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(
                                DispatcherType.ASYNC,
                                DispatcherType.ERROR
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/auth/signup",
                                "/auth/login").permitAll()
                        .requestMatchers("/admin/**").authenticated()
                        .requestMatchers(HttpMethod.GET,
                                "/auth/me").authenticated()
                        .requestMatchers("/bookings", "/bookings/**").authenticated()
                        .requestMatchers(HttpMethod.GET,
                                "/hotels/search").permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/payments/success",
                                "/payments/cancel").permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/webhooks/stripe").permitAll()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults()))
                .build();

    }
}
