package com.alvaro.pricewise.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Configuration
public class MetricsConfig {

    @Bean
    public Counter keepaRequestsSuccess(MeterRegistry registry) {
        return Counter.builder("pricewise.keepa.requests")
                .tag("result", "success")
                .description("Keepa API successful requests")
                .register(registry);
    }

    @Bean
    public Counter keepaRequestsError(MeterRegistry registry) {
        return Counter.builder("pricewise.keepa.requests")
                .tag("result", "error")
                .description("Keepa API failed requests")
                .register(registry);
    }

    @Bean
    public Timer keepaDuration(MeterRegistry registry) {
        return Timer.builder("pricewise.keepa.duration")
                .description("Keepa API request duration")
                .register(registry);
    }

    @Bean
    public Timer schedulerDuration(MeterRegistry registry) {
        return Timer.builder("pricewise.scheduler.duration")
                .description("Scheduled job execution duration")
                .register(registry);
    }
}
