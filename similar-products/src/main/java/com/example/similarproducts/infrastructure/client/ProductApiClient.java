package com.example.similarproducts.infrastructure.client;

import com.example.similarproducts.domain.exception.ProductNotFoundException;
import com.example.similarproducts.domain.exception.UpstreamServerException;
import com.example.similarproducts.domain.model.Product;
import com.example.similarproducts.domain.model.ProductId;
import com.example.similarproducts.application.port.out.ProductDetailProvider;
import com.example.similarproducts.application.port.out.SimilarProductIdsProvider;
import com.example.similarproducts.infrastructure.cache.ReactiveCache;
import com.example.similarproducts.infrastructure.client.dto.ProductDetailDto;
import com.example.similarproducts.infrastructure.client.mapper.ProductDtoMapper;
import com.example.similarproducts.infrastructure.resilience.ResilienceDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Single outbound adapter that implements both driven ports over the same {@link WebClient}.
 *
 * <p>Each call is composed as: <b>cache → resilience → HTTP</b>. The cache sits <i>outside</i>
 * resilience on purpose: a cache hit returns immediately and does not consume a circuit-breaker
 * or bulkhead permit, so cached data is still served even while the circuit is open.
 *
 * <p>HTTP status handling is normalised into domain exceptions here so the resilience layer can
 * treat them correctly: 404 → {@link ProductNotFoundException} (never retried, never trips the
 * breaker); any other error status / transport failure → {@link UpstreamServerException}
 * (retryable, counts toward the breaker).
 */
@Component
public class ProductApiClient implements SimilarProductIdsProvider, ProductDetailProvider {

    private static final Logger log = LoggerFactory.getLogger(ProductApiClient.class);

    private static final String SIMILAR_IDS_INSTANCE = "similar-ids";
    private static final String PRODUCT_DETAIL_INSTANCE = "product-detail";

    private final WebClient webClient;
    private final ResilienceDecorator resilience;
    private final ReactiveCache<List<ProductId>> similarIdsCache;
    private final ReactiveCache<Product> productDetailCache;
    private final ProductDtoMapper mapper;

    public ProductApiClient(WebClient productApiWebClient,
                            ResilienceDecorator resilience,
                            ReactiveCache<List<ProductId>> similarIdsCache,
                            ReactiveCache<Product> productDetailCache,
                            ProductDtoMapper mapper) {
        this.webClient = productApiWebClient;
        this.resilience = resilience;
        this.similarIdsCache = similarIdsCache;
        this.productDetailCache = productDetailCache;
        this.mapper = mapper;
    }

    @Override
    public Mono<List<ProductId>> getSimilarIds(ProductId productId) {
        return similarIdsCache.get(productId.value(),
                key -> resilience.decorate(SIMILAR_IDS_INSTANCE, fetchSimilarIds(key)));
    }

    @Override
    public Mono<Product> getById(ProductId productId) {
        return productDetailCache.get(productId.value(),
                key -> resilience.decorate(PRODUCT_DETAIL_INSTANCE, fetchDetail(key)));
    }

    private Mono<List<ProductId>> fetchSimilarIds(String id) {
        return webClient.get()
                .uri("/product/{productId}/similarIds", id)
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        response -> Mono.error(new ProductNotFoundException(id)))
                .onStatus(HttpStatusCode::isError,
                        response -> Mono.error(new UpstreamServerException(
                                "similar-ids upstream returned " + response.statusCode())))
                .bodyToMono(String[][].class)
                .map(mapper::toProductIds)
                .onErrorMap(this::isTransportError,
                        e -> new UpstreamServerException("similar-ids transport failure", e))
                // Fires only on a cache miss (the loader is not subscribed on a hit), so this
                // traces genuine upstream traffic.
                .doFirst(() -> log.debug("Calling upstream GET /product/{}/similarIds", id));
    }

    private Mono<Product> fetchDetail(String id) {
        return webClient.get()
                .uri("/product/{productId}", id)
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        response -> Mono.error(new ProductNotFoundException(id)))
                .onStatus(HttpStatusCode::isError,
                        response -> Mono.error(new UpstreamServerException(
                                "product-detail upstream returned " + response.statusCode())))
                .bodyToMono(ProductDetailDto.class)
                .map(mapper::toDomain)
                .onErrorMap(this::isTransportError,
                        e -> new UpstreamServerException("product-detail transport failure", e))
                .doFirst(() -> log.debug("Calling upstream GET /product/{}", id));
    }

    /** Connection resets / DNS / read failures surface as WebClientRequestException. */
    private boolean isTransportError(Throwable t) {
        return t instanceof org.springframework.web.reactive.function.client.WebClientRequestException;
    }
}
