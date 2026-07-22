package com.example.similarproducts.infrastructure.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Assigns a correlation id to every request and logs its start and completion.
 *
 * <p>Runs first ({@link Ordered#HIGHEST_PRECEDENCE}) so the id is in scope for the whole chain.
 * If the caller already supplied {@code X-Correlation-Id} it is reused (so a trace can be followed
 * across services); otherwise a new one is generated. The id is written into the Reactor
 * {@code Context} at the bottom of the chain, echoed back on the response header, and — via
 * {@link MdcCorrelationIdAccessor} — surfaced in the MDC of every log line downstream.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdWebFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String correlationId = resolveCorrelationId(request);
        exchange.getResponse().getHeaders().set(CorrelationId.HEADER, correlationId);

        String method = request.getMethod().name();
        String path = request.getPath().pathWithinApplication().value();
        long startNanos = System.nanoTime();

        return chain.filter(exchange)
                .doFirst(() -> log.info("Request received: {} {}", method, path))
                .doOnError(error ->
                        log.error("Request failed: {} {} - {}", method, path, error.toString()))
                .doFinally(signal -> {
                    long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
                    log.info("Request completed: {} {} -> {} in {} ms ({})",
                            method, path, exchange.getResponse().getStatusCode(), elapsedMs, signal);
                })
                // Placed last so the id is visible to the entire chain above.
                .contextWrite(ctx -> ctx.put(CorrelationId.KEY, correlationId));
    }

    private String resolveCorrelationId(ServerHttpRequest request) {
        String incoming = request.getHeaders().getFirst(CorrelationId.HEADER);
        return (incoming == null || incoming.isBlank()) ? UUID.randomUUID().toString() : incoming;
    }
}
