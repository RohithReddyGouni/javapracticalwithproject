package com.rohith.javaprojects.airbnb.aspect;

import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class ServiceTimingAspect {

    private static final Logger log =
            LoggerFactory.getLogger(ServiceTimingAspect.class);

    private final MeterRegistry meterRegistry;

    public ServiceTimingAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Around(
            "execution(public * com.rohith.javaprojects.airbnb.service..*.*(..))")
    public Object measureExecutionTime(
            ProceedingJoinPoint joinPoint) throws Throwable {

        long startedAt = System.nanoTime();

        String service =
                joinPoint.getSignature().getDeclaringTypeName();

        String method =
                joinPoint.getSignature().getName();

        String operation =
                joinPoint.getSignature().toShortString();

        Object result;

        try {
            result = joinPoint.proceed();

        } catch (Throwable failure) {

            recordTiming(
                    service,
                    method,
                    operation,
                    startedAt,
                    false);

            throw failure;
        }

        if (result instanceof CompletionStage<?> stage) {

            stage.whenComplete((value, failure) ->
                    recordTiming(
                            service,
                            method,
                            operation,
                            startedAt,
                            failure == null));

        } else {

            recordTiming(
                    service,
                    method,
                    operation,
                    startedAt,
                    true);
        }

        return result;
    }

    private void recordTiming(
            String service,
            String method,
            String operation,
            long startedAt,
            boolean successful) {

        long durationNanos = System.nanoTime() - startedAt;

        try {
            meterRegistry.timer(
                    "app.service.execution",
                    "service", service,
                    "method", method,
                    "outcome", successful ? "success" : "failure"
            ).record(durationNanos, TimeUnit.NANOSECONDS);

            log.debug(
                    "method={} durationMs={} successful={}",
                    operation,
                    TimeUnit.NANOSECONDS.toMillis(durationNanos),
                    successful);

        } catch (RuntimeException metricsException) {

            log.warn(
                    "Could not record service timing: {}",
                    metricsException.getClass().getSimpleName());
        }
    }
}