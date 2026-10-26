package com.rohith.javaprojects.airbnb.config;

import com.rohith.javaprojects.airbnb.dto.HotelDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisCacheConfig implements CachingConfigurer {

    @Bean
    public RedisCacheManager cacheManager(
            @Qualifier("cacheRedisConnectionFactory")
            RedisConnectionFactory connectionFactory) {

        var hotelSerializer =
                new JacksonJsonRedisSerializer<>(HotelDTO.class);

        RedisCacheConfiguration hotelCache =
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofSeconds(60))
                        .disableCachingNullValues()
                        .computePrefixWith(
                                cacheName ->
                                        "airbnb:v1:" + cacheName + "::")
                        .serializeKeysWith(
                                RedisSerializationContext.SerializationPair
                                        .fromSerializer(
                                                new StringRedisSerializer()))
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair
                                        .fromSerializer(hotelSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .withCacheConfiguration("hotelByOwner", hotelCache)
                .disableCreateOnMissingCache()
                .transactionAware()
                .build();
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new HotelCacheErrorHandler();
    }
}