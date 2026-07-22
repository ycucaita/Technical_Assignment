package com.example.similarproducts.infrastructure.resilience;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Applies the full resilience stack to a reactive call, composed as explicit Reactor operators.
 *
 * <p>Order of concerns, from the actual call outward (mirrors Resilience4j's documented aspect
 * order {@code Retry(CircuitBreaker(TimeLimiter(Bulkhead(call))))}):
 * <pre>
 *   call
 *     -> Bulkhead      (cap concurrent in-flight calls to the dependency)
 *     -> timeout       (hard per-call ceiling; a timeout counts as a failure)
 *     -> CircuitBreaker(fail fast while the dependency is unhealthy)
 *     -> Retry         (outermost; re-attempts transient failures with backoff)
 * </pre>
 * Because the circuit breaker sits outside the bulkhead, an open circuit rejects the call before a
 * bulkhead permit is even acquired. Retry is configured (in {@code ResilienceConfig}) to ignore
 * "not found", "circuit open" and "bulkhead full", so it only re-attempts genuine transient faults.
 */
@Component
public class ResilienceDecorator {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final BulkheadRegistry bulkheadRegistry;
    private final Duration callTimeout;

    public ResilienceDecorator(CircuitBreakerRegistry circuitBreakerRegistry,
                               RetryRegistry retryRegistry,
                               BulkheadRegistry bulkheadRegistry,
                               Duration resilienceCallTimeout) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.bulkheadRegistry = bulkheadRegistry;
        this.callTimeout = resilienceCallTimeout;
    }

    public <T> Mono<T> decorate(String instanceName, Mono<T> action) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(instanceName);
        Retry retry = retryRegistry.retry(instanceName);
        Bulkhead bulkhead = bulkheadRegistry.bulkhead(instanceName);

        return action
                .transformDeferred(BulkheadOperator.of(bulkhead))
                .timeout(callTimeout)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry));
    }
}
