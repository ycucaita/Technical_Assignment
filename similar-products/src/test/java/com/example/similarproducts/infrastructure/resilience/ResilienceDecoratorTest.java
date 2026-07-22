package com.example.similarproducts.infrastructure.resilience;

import com.example.similarproducts.domain.exception.ProductNotFoundException;
import com.example.similarproducts.domain.exception.UpstreamServerException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ResilienceDecoratorTest {

    private static final String INSTANCE = "test";

    private ResilienceDecorator decorator(CircuitBreakerRegistry cb, RetryRegistry retry, Duration timeout) {
        return new ResilienceDecorator(cb, retry, BulkheadRegistry.ofDefaults(), timeout);
    }

    private CircuitBreakerRegistry cbRegistry(int window) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(window)
                .minimumNumberOfCalls(window)
                .failureRateThreshold(50f)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .ignoreExceptions(ProductNotFoundException.class)
                .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        registry.circuitBreaker(INSTANCE, config);
        return registry;
    }

    private RetryRegistry retryRegistry(int maxAttempts) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .waitDuration(Duration.ofMillis(1))
                .retryExceptions(UpstreamServerException.class, TimeoutException.class)
                .build();
        RetryRegistry registry = RetryRegistry.ofDefaults();
        registry.retry(INSTANCE, config);
        return registry;
    }

    @Test
    void circuitOpensAfterFailureThresholdAndThenFailsFast() {
        CircuitBreakerRegistry cbRegistry = cbRegistry(5);
        ResilienceDecorator decorator = decorator(cbRegistry, retryRegistry(1), Duration.ofSeconds(5));

        // Drive 5 failing calls to fill the window and trip the breaker.
        for (int i = 0; i < 5; i++) {
            StepVerifier.create(decorator.decorate(INSTANCE,
                            Mono.<String>error(new UpstreamServerException("boom"))))
                    .expectError(UpstreamServerException.class)
                    .verify();
        }

        assertThat(cbRegistry.circuitBreaker(INSTANCE).getState())
                .isEqualTo(CircuitBreaker.State.OPEN);

        // Next call is rejected without touching the (would-be) action.
        AtomicInteger invocations = new AtomicInteger();
        StepVerifier.create(decorator.decorate(INSTANCE,
                        Mono.fromSupplier(() -> {
                            invocations.incrementAndGet();
                            return "should-not-run";
                        })))
                .expectError(CallNotPermittedException.class)
                .verify();

        assertThat(invocations.get()).isZero();
    }

    @Test
    void retriesTransientFailuresUpToMaxAttempts() {
        ResilienceDecorator decorator =
                decorator(cbRegistry(100), retryRegistry(3), Duration.ofSeconds(5));

        AtomicInteger attempts = new AtomicInteger();
        Mono<String> action = Mono.defer(() -> {
            attempts.incrementAndGet();
            return Mono.error(new UpstreamServerException("transient"));
        });

        StepVerifier.create(decorator.decorate(INSTANCE, action))
                .expectError(UpstreamServerException.class)
                .verify();

        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void notFoundIsNotRetriedAndDoesNotOpenTheCircuit() {
        CircuitBreakerRegistry cbRegistry = cbRegistry(5);
        ResilienceDecorator decorator = decorator(cbRegistry, retryRegistry(3), Duration.ofSeconds(5));

        AtomicInteger attempts = new AtomicInteger();
        Mono<String> action = Mono.defer(() -> {
            attempts.incrementAndGet();
            return Mono.error(new ProductNotFoundException("42"));
        });

        StepVerifier.create(decorator.decorate(INSTANCE, action))
                .expectError(ProductNotFoundException.class)
                .verify();

        assertThat(attempts.get()).isEqualTo(1);
        assertThat(cbRegistry.circuitBreaker(INSTANCE).getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void slowCallTimesOut() {
        ResilienceDecorator decorator =
                decorator(cbRegistry(100), retryRegistry(1), Duration.ofMillis(100));

        StepVerifier.create(decorator.decorate(INSTANCE, Mono.never()))
                .expectError(TimeoutException.class)
                .verify(Duration.ofSeconds(2));
    }
}
