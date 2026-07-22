package com.example.similarproducts.infrastructure.cache;

import com.github.benmanes.caffeine.cache.AsyncCache;

import java.util.function.Function;

import reactor.core.publisher.Mono;

/**
 * Thin reactive wrapper over a Caffeine {@link AsyncCache}.
 *
 * <p>Why an async cache rather than a plain map or {@code @Cacheable}:
 * <ul>
 *   <li><b>Stampede protection</b> — Caffeine coalesces concurrent loads for the same key onto a
 *       single in-flight {@code CompletableFuture}, so a burst of identical requests under load
 *       triggers exactly one upstream call. This is the key property for the heavy-load scenario.</li>
 *   <li><b>Failure is not cached</b> — if the loader errors, Caffeine evicts the failed future, so
 *       transient upstream errors are never "stuck" in the cache.</li>
 *   <li>Spring's cache abstraction historically does not handle reactive return types correctly
 *       (it would cache the {@code Mono} publisher itself), so we do not rely on {@code @Cacheable}.</li>
 * </ul>
 *
 * @param <V> cached value type
 */
public class ReactiveCache<V> {

    private final AsyncCache<String, V> cache;

    public ReactiveCache(AsyncCache<String, V> cache) {
        this.cache = cache;
    }

    /**
     * Returns the cached value for {@code key}, invoking {@code loader} once on a miss.
     * Concurrent callers for the same missing key share a single loader invocation.
     */
    public Mono<V> get(String key, Function<String, Mono<V>> loader) {
        return Mono.defer(() ->
                Mono.fromFuture(cache.get(key, (k, executor) -> loader.apply(k).toFuture())));
    }
}
