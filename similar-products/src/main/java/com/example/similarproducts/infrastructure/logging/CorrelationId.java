package com.example.similarproducts.infrastructure.logging;

/**
 * Shared constants for request correlation.
 *
 * <p>The same string is used as both the MDC key and the Reactor {@code Context} key. That is
 * deliberate: Micrometer's context-propagation matches a {@code ThreadLocalAccessor} to a Reactor
 * context entry by key, so keeping them identical is what lets the id flow
 * Reactor Context → ThreadLocal (MDC) → log line as the pipeline hops threads.
 */
public final class CorrelationId {

    /** MDC + Reactor context key. Referenced by the logback pattern as {@code %X{correlationId}}. */
    public static final String KEY = "correlationId";

    /** Inbound/outbound HTTP header carrying the id, so tracing spans service boundaries. */
    public static final String HEADER = "X-Correlation-Id";

    private CorrelationId() {
    }
}
