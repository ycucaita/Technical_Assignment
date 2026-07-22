package com.example.similarproducts.application.service;

import com.example.similarproducts.domain.model.Product;
import com.example.similarproducts.domain.model.ProductId;
import com.example.similarproducts.application.port.in.GetSimilarProductsUseCase;
import com.example.similarproducts.application.port.out.ProductDetailProvider;
import com.example.similarproducts.application.port.out.SimilarProductIdsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Orchestrates the two upstream calls into a single aggregated response.
 *
 * <p>Design notes:
 * <ul>
 *   <li>Framework-free POJO: it depends only on domain ports, so it is trivially unit-testable
 *       without Spring. It is wired as a bean in {@code config.UseCaseConfig}.</li>
 *   <li>Fan-out uses {@code flatMapSequential} with a bounded concurrency: detail look-ups run
 *       in parallel (fast) while the emission order still mirrors the upstream id order.</li>
 *   <li>Graceful degradation: a single detail failure (404, timeout, open circuit, 5xx) is
 *       swallowed and replaced by an id-only product, so one bad dependency never fails the
 *       whole response. Failing to obtain the id list itself has no fallback and propagates.</li>
 * </ul>
 */
public class GetSimilarProductsService implements GetSimilarProductsUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetSimilarProductsService.class);

    private final SimilarProductIdsProvider similarIdsProvider;
    private final ProductDetailProvider productDetailProvider;
    private final int detailConcurrency;

    public GetSimilarProductsService(SimilarProductIdsProvider similarIdsProvider,
                                     ProductDetailProvider productDetailProvider,
                                     int detailConcurrency) {
        this.similarIdsProvider = similarIdsProvider;
        this.productDetailProvider = productDetailProvider;
        this.detailConcurrency = Math.max(1, detailConcurrency);
    }

    @Override
    public Flux<Product> getSimilarProducts(ProductId productId) {
        return similarIdsProvider.getSimilarIds(productId)
                .doOnSubscribe(s -> log.info("Resolving similar products for {}", productId))
                .doOnNext(ids -> log.info("Found {} similar id(s) for {}", ids.size(), productId))
                .flatMapMany(ids -> Flux.fromIterable(ids)
                        .flatMapSequential(this::resolveDetailDegrading, detailConcurrency))
                .doOnComplete(() -> log.info("Finished aggregating similar products for {}", productId))
                .doOnError(error ->
                        log.warn("Could not resolve similar-id list for {}: {}", productId, error.toString()));
    }

    /**
     * Resolves a single detail, degrading to an id-only product on ANY failure so that
     * a partial upstream outage still yields a complete list. The degradation is logged at WARN
     * so a partial outage is visible even when the overall response is a success.
     */
    private Mono<Product> resolveDetailDegrading(ProductId id) {
        return productDetailProvider.getById(id)
                .onErrorResume(error -> {
                    log.warn("Degrading product {} to id-only due to: {}", id, error.toString());
                    return Mono.just(Product.idOnly(id));
                });
    }
}
