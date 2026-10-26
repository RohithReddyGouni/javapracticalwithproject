package com.rohith.javaprojects.airbnb.aspect;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class HotelLookupTracingAspect {

    private final Tracer tracer;

    public HotelLookupTracingAspect(Tracer tracer) {
        this.tracer = tracer;
    }

    @Around(
            "execution(public * " +
            "com.rohith.javaprojects.airbnb.service." +
            "HotelServiceImpl.getHotelById(..))"
    )
    public Object traceHotelLookup(
            ProceedingJoinPoint joinPoint) throws Throwable {

        Span span = tracer.nextSpan()
                .name("hotel.lookup")
                .start();

        try (Tracer.SpanInScope scope = tracer.withSpan(span)) {
            return joinPoint.proceed();
        } catch (Throwable exception) {
            span.error(exception);
            throw exception;
        } finally {
            span.end();
        }
    }
}