package com.example.similarproducts.config;

import com.example.similarproducts.config.properties.ProductApiProperties;
import com.example.similarproducts.config.properties.ResilienceProperties;
import com.example.similarproducts.domain.exception.ProductNotFoundException;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedBulkheadMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Builds the Resilience4j registries from typed properties and eagerly instantiates one named
 * instance per outbound dependency, so metrics and state exist from start-up.
 *
 * <p>Key correctness decisions:
 * <ul>
 *   <li>{@link ProductNotFoundException} (404) is <b>ignored</b> by the breaker — a missing product
 *       is a normal answer, not a fault — and is <b>never retried</b>.</li>
 *   <li>Retry only re-attempts genuine transient faults ({@link TimeoutException},
 *       {@code UpstreamServerException}/5xx, {@link WebClientRequestException}); it ignores
 *       {@link CallNotPermittedException} (open circuit) and {@link BulkheadFullException} so it
 *       does not hammer a dependency that is already shedding load.</li>
 *   <li>Slow calls (breaching {@code slow-call-duration-threshold}) count toward opening the
 *       circuit, so a chronically slow-but-not-failing upstream still trips the breaker.</li>
 * </ul>
 */
@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(ResilienceProperties props) {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        props.instances().forEach((name, spec) ->
                registry.circuitBreaker(name, circuitBreakerConfig(spec.circuitBreaker())));
        return registry;
    }

    @Bean
    public RetryRegistry retryRegistry(ResilienceProperties props) {
        RetryRegistry registry = RetryRegistry.ofDefaults();
        props.instances().forEach((name, spec) ->
                registry.retry(name, retryConfig(spec.retry())));
        return registry;
    }

    @Bean
    public BulkheadRegistry bulkheadRegistry(ResilienceProperties props) {
        BulkheadRegistry registry = BulkheadRegistry.ofDefaults();
        props.instances().forEach((name, spec) ->
                registry.bulkhead(name, bulkheadConfig(spec.bulkhead())));
        return registry;
    }

    /** Hard per-call timeout shared by the resilience pipeline. */
    @Bean
    public Duration resilienceCallTimeout(ProductApiProperties props) {
        return props.callTimeout();
    }

    private CircuitBreakerConfig circuitBreakerConfig(ResilienceProperties.CircuitBreakerSpec s) {
        return CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(s.slidingWindowSize())
                .failureRateThreshold(s.failureRateThreshold())
                .slowCallDurationThreshold(s.slowCallDurationThreshold())
                .slowCallRateThreshold(s.slowCallRateThreshold())
                .waitDurationInOpenState(s.waitDurationInOpenState())
                .permittedNumberOfCallsInHalfOpenState(s.permittedCallsInHalfOpenState())
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .ignoreExceptions(ProductNotFoundException.class)
                .build();
    }

    private RetryConfig retryConfig(ResilienceProperties.RetrySpec s) {
        return RetryConfig.custom()
                .maxAttempts(s.maxAttempts())
                .waitDuration(s.waitDuration())
                .retryExceptions(
                        TimeoutException.class,
                        com.example.similarproducts.domain.exception.UpstreamServerException.class,
                        WebClientRequestException.class)
                .ignoreExceptions(
                        ProductNotFoundException.class,
                        CallNotPermittedException.class,
                        BulkheadFullException.class)
                .build();
    }

    private BulkheadConfig bulkheadConfig(ResilienceProperties.BulkheadSpec s) {
        return BulkheadConfig.custom()
                .maxConcurrentCalls(s.maxConcurrentCalls())
                .maxWaitDuration(s.maxWaitDuration())
                .build();
    }

    // ---- Micrometer bindings (auto-registered because they are MeterBinder beans) ----

    @Bean
    public MeterBinder circuitBreakerMetrics(CircuitBreakerRegistry registry) {
        return TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry);
    }

    @Bean
    public MeterBinder retryMetrics(RetryRegistry registry) {
        return TaggedRetryMetrics.ofRetryRegistry(registry);
    }

    @Bean
    public MeterBinder bulkheadMetrics(BulkheadRegistry registry) {
        return TaggedBulkheadMetrics.ofBulkheadRegistry(registry);
    }
}
