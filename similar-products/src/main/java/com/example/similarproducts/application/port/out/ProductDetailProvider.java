package com.example.similarproducts.application.port.out;

import com.example.similarproducts.domain.model.Product;
import com.example.similarproducts.domain.model.ProductId;
import reactor.core.publisher.Mono;

/**
 * Driven port for the "product detail" upstream endpoint.
 */
public interface ProductDetailProvider {

    Mono<Product> getById(ProductId productId);
}
