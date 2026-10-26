package com.rohith.javaprojects.airbnb.security;

import com.rohith.javaprojects.airbnb.advice.ApiError;
import com.rohith.javaprojects.airbnb.dto.RateLimitDecision;
import com.rohith.javaprojects.airbnb.service.RedisRateLimitService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HotelSearchRateLimitFilter
        extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(HotelSearchRateLimitFilter.class);

    private final RedisRateLimitService rateLimitService;
    private final JsonMapper jsonMapper;
    private final int limit;
    private final long windowMillis;

    public HotelSearchRateLimitFilter(
            RedisRateLimitService rateLimitService,
            JsonMapper jsonMapper,
            int limit,
            long windowMillis) {

        if (limit < 1
                || windowMillis < 1
                || windowMillis > 3_600_000) {

            throw new IllegalArgumentException(
                    "Rate limit must be positive and window must be 1ms–1h");
        }

        this.rateLimitService = rateLimitService;
        this.jsonMapper = jsonMapper;
        this.limit = limit;
        this.windowMillis = windowMillis;
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request) {

        return !"GET".equals(request.getMethod())
                || !"/hotels/search".equals(
                        request.getServletPath());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String key = "airbnb:ratelimit:v1:hotel-search:ip:"
                + request.getRemoteAddr();

        RateLimitDecision decision;

        try {
            decision = rateLimitService.check(
                    key,
                    limit,
                    windowMillis);

        } catch (CallNotPermittedException exception) {

            writeError(
                    response,
                    503,
                    "Hotel search is temporarily unavailable. Please try again.");

            return;

        } catch (DataAccessException exception) {

            log.warn(
                    "Hotel search rate limiter unavailable: errorType={}",
                    exception.getClass().getSimpleName());

            writeError(
                    response,
                    503,
                    "Hotel search is temporarily unavailable. Please try again.");

            return;
        }

        response.setHeader(
                "X-RateLimit-Limit",
                Integer.toString(limit));

        response.setHeader(
                "X-RateLimit-Remaining",
                Long.toString(decision.remainingRequests()));

        if (!decision.allowed()) {

            long retryAfterSeconds = Math.max(
                    1,
                    (decision.remainingWindowMillis() + 999) / 1000);

            response.setHeader(
                    HttpHeaders.RETRY_AFTER,
                    Long.toString(retryAfterSeconds));

            writeError(
                    response,
                    429,
                    "Too many hotel searches. Please try again later.");

            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeError(
            HttpServletResponse response,
            int status,
            String message) throws IOException {

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");

        ApiError error = new ApiError(
                status,
                message,
                Map.of());

        response.getWriter().write(
                jsonMapper.writeValueAsString(error));
    }
}