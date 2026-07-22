package com.example.similarproducts.application.port.in;

import com.example.similarproducts.domain.model.Product;
import com.example.similarproducts.domain.model.ProductId;
import reactor.core.publisher.Flux;

/**
 * Driving port: the single business capability this service exposes.
 */
public interface GetSimilarProductsUseCase {

    /**
     * Returns the detail of every product similar to the given one, in the order the
     * upstream returns the similar ids. Individual detail failures degrade to id-only
     * products; a failure to obtain the similar-id list propagates as an error.
     */
    Flux<Product> getSimilarProducts(ProductId productId);
}
