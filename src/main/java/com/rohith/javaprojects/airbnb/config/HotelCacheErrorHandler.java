package com.rohith.javaprojects.airbnb.config;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;

public class HotelCacheErrorHandler implements CacheErrorHandler {

    private static final Logger log =
            LoggerFactory.getLogger(HotelCacheErrorHandler.class);

    @Override
    public void handleCacheGetError(
            RuntimeException exception,
            Cache cache,
            Object key) {

        handleFailure("GET", exception, cache);
    }

    @Override
    public void handleCachePutError(
            RuntimeException exception,
            Cache cache,
            Object key,
            @Nullable Object value) {

        handleFailure("PUT", exception, cache);
    }

    @Override
    public void handleCacheEvictError(
            RuntimeException exception,
            Cache cache,
            Object key) {

        handleFailure("EVICT", exception, cache);
    }

    @Override
    public void handleCacheClearError(
            RuntimeException exception,
            Cache cache) {

        handleFailure("CLEAR", exception, cache);
    }

    private void handleFailure(
            String operation,
            RuntimeException exception,
            Cache cache) {

        boolean hotelCache =
                "hotelByOwner".equals(cache.getName());

        boolean unavailable =
                exception instanceof RedisConnectionFailureException
                        || exception instanceof QueryTimeoutException;

        if (!hotelCache || !unavailable) {
            throw exception;
        }

        log.warn(
                "Hotel cache unavailable: operation={} cache={} errorType={}",
                operation,
                cache.getName(),
                exception.getClass().getSimpleName());
    }
}