package com.example.similarproducts.infrastructure.cache;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ReactiveCacheTest {

    private <V> ReactiveCache<V> newCache() {
        AsyncCache<String, V> caffeine = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(Duration.ofSeconds(30))
                .buildAsync();
        return new ReactiveCache<>(caffeine);
    }

    @Test
    void loaderIsInvokedOnceThenValueIsServedFromCache() {
        ReactiveCache<String> cache = newCache();
        AtomicInteger loads = new AtomicInteger();

        Mono<String> first = cache.get("k", key -> Mono.fromSupplier(() -> {
            loads.incrementAndGet();
            return "value";
        }));

        StepVerifier.create(first).expectNext("value").verifyComplete();
        StepVerifier.create(cache.get("k", key -> Mono.just("SHOULD-NOT-BE-CALLED")))
                .expectNext("value")
                .verifyComplete();

        assertThat(loads.get()).isEqualTo(1);
    }

    @Test
    void concurrentMissesForSameKeyCoalesceIntoOneLoad() {
        ReactiveCache<String> cache = newCache();
        AtomicInteger loads = new AtomicInteger();

        var loader = (java.util.function.Function<String, Mono<String>>) key ->
                Mono.delay(Duration.ofMillis(50))
                        .map(ignored -> {
                            loads.incrementAndGet();
                            return "value";
                        });

        Mono<String> a = cache.get("k", loader);
        Mono<String> b = cache.get("k", loader);

        StepVerifier.create(Mono.zip(a, b))
                .assertNext(tuple -> {
                    assertThat(tuple.getT1()).isEqualTo("value");
                    assertThat(tuple.getT2()).isEqualTo("value");
                })
                .verifyComplete();

        assertThat(loads.get()).isEqualTo(1);
    }

    @Test
    void failedLoadIsNotCached() {
        ReactiveCache<String> cache = newCache();
        AtomicInteger loads = new AtomicInteger();

        Mono<String> failing = cache.get("k", key -> Mono.defer(() -> {
            loads.incrementAndGet();
            return Mono.error(new IllegalStateException("boom"));
        }));

        StepVerifier.create(failing).expectError(IllegalStateException.class).verify();

        StepVerifier.create(cache.get("k", key -> Mono.just("recovered")))
                .expectNext("recovered")
                .verifyComplete();

        assertThat(loads.get()).isEqualTo(1);
    }
}
