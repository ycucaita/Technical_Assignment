package com.example.similarproducts.application.port.out;

import com.example.similarproducts.domain.model.ProductId;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Driven port for the "similar ids" upstream endpoint. Segregated from the detail port
 * (ISP) because callers of one rarely need the other.
 */
public interface SimilarProductIdsProvider {

    Mono<List<ProductId>> getSimilarIds(ProductId productId);
}
