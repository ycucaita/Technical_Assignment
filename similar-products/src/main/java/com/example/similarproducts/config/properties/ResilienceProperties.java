package com.example.similarproducts.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * Resilience settings keyed by instance name (e.g. "similar-ids", "product-detail").
 */
@ConfigurationProperties(prefix = "resilience")
public record ResilienceProperties(Map<String, InstanceSpec> instances) {

    public record InstanceSpec(CircuitBreakerSpec circuitBreaker, RetrySpec retry, BulkheadSpec bulkhead) {
    }

    public record CircuitBreakerSpec(
            int slidingWindowSize,
            float failureRateThreshold,
            Duration slowCallDurationThreshold,
            float slowCallRateThreshold,
            Duration waitDurationInOpenState,
            int permittedCallsInHalfOpenState
    ) {
    }

    public record RetrySpec(int maxAttempts, Duration waitDuration) {
    }

    public record BulkheadSpec(int maxConcurrentCalls, Duration maxWaitDuration) {
    }
}
