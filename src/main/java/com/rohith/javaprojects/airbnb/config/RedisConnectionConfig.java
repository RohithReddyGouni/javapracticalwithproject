package com.rohith.javaprojects.airbnb.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Configuration
@EnableConfigurationProperties(DataRedisProperties.class)
public class RedisConnectionConfig {

    @Bean
    public LettuceClientConfiguration redisClientConfiguration(
            DataRedisProperties properties) {

        Duration connectTimeout = Objects.requireNonNullElse(
                properties.getConnectTimeout(),
                Duration.ofSeconds(2));

        Duration commandTimeout = Objects.requireNonNullElse(
                properties.getTimeout(),
                Duration.ofSeconds(1));

        ClientOptions options = ClientOptions.builder()
                .socketOptions(SocketOptions.builder()
                        .connectTimeout(connectTimeout)
                        .build())
                .timeoutOptions(TimeoutOptions.enabled())
                .build();

        return LettuceClientConfiguration.builder()
                .clientOptions(options)
                .commandTimeout(commandTimeout)
                .build();
    }

    @Bean
    @Primary
    public LettuceConnectionFactory cacheRedisConnectionFactory(
            DataRedisProperties properties,
            LettuceClientConfiguration clientConfiguration) {

        var server = new RedisStandaloneConfiguration(
                properties.getHost(),
                properties.getPort());

        return new LettuceConnectionFactory(
                server,
                clientConfiguration);
    }

    @Bean
    public LettuceConnectionFactory rateLimitRedisConnectionFactory(
            @Value("${app.rate-limit.redis.host}") String host,
            @Value("${app.rate-limit.redis.port}") int port,
            LettuceClientConfiguration clientConfiguration) {

        var server = new RedisStandaloneConfiguration(host, port);

        return new LettuceConnectionFactory(
                server,
                clientConfiguration);
    }

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate(
            @Qualifier("cacheRedisConnectionFactory")
            RedisConnectionFactory connectionFactory) {

        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public StringRedisTemplate rateLimitRedisTemplate(
            @Qualifier("rateLimitRedisConnectionFactory")
            RedisConnectionFactory connectionFactory) {

        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public RedisScript<List> rateLimitScript() {

        DefaultRedisScript<List> script = new DefaultRedisScript<>();

        script.setLocation(new ClassPathResource(
                "redis/fixed-window-rate-limit.lua"));

        script.setResultType(List.class);

        return script;
    }
}