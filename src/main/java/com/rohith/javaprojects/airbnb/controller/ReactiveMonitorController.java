package com.rohith.javaprojects.airbnb.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/admin/monitoring")
public class ReactiveMonitorController {

    private static final Logger log =
            LoggerFactory.getLogger(ReactiveMonitorController.class);
    private final WebClient webClient;
    private final String prometheusUrl;
    private final String zipkinUrl;


    public ReactiveMonitorController(

            WebClient.Builder builder,
            @Value("${app.reactive.prometheus-url:http://prometheus:9090/-/ready}")
            String prometheusUrl,
            @Value("${app.reactive.zipkin-url:http://zipkin:9411/health}")
            String zipkinUrl) {
        this.webClient = builder.build();
        this.prometheusUrl = prometheusUrl;
        this.zipkinUrl = zipkinUrl;
    }


    @GetMapping("/status")
    public Mono<MonitoringStatus> getStatus() {
        Mono<DependencyStatus> prometheus =
                check("prometheus", prometheusUrl);
        Mono<DependencyStatus> zipkin =
                check("zipkin", zipkinUrl);

        return Mono.zip(prometheus, zipkin)
                .map(results -> new MonitoringStatus(
                        results.getT1(),
                        results.getT2(),
                        Instant.now()
                ));
    }

    @GetMapping(
            value = "/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MonitoringStatus> streamStatus() {
        return Flux.range(1,5)
                .concatMap(number ->
                        getStatus()
                                .delayElement(Duration.ofSeconds(1)));
    }

    private Mono<DependencyStatus> check(
            String name,
            String url) {

        return webClient.get()
                .uri(url)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(2))
                .map(response -> new DependencyStatus(
                        name,
                        response.getStatusCode().is2xxSuccessful()
                        ? "UP"
                                : "UNAVAILABLE"))
                .onErrorResume(error ->
                        Mono.just(new DependencyStatus(
                                name,
                                "UNAVAILABLE")))
                .doOnNext(result -> log.info(
                        "{} -> {} on thread {}",
                        name,
                        result.status(),
                        Thread.currentThread().getName()));
    }


    public record DependencyStatus(
            String name,
            String status) {

    }


    public record MonitoringStatus(
            DependencyStatus prometheus,
            DependencyStatus zipkin,
            Instant checkedAt) {

    }

}
