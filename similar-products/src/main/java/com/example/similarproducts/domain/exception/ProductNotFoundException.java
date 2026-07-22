package com.example.similarproducts.domain.exception;

/**
 * Raised when an upstream endpoint returns 404 for a given product id.
 * Treated as a normal outcome (never counts as a circuit-breaker failure and is never retried).
 */
public class ProductNotFoundException extends RuntimeException {

    private final String productId;

    public ProductNotFoundException(String productId) {
        super("Product not found: " + productId);
        this.productId = productId;
    }

    public String getProductId() {
        return productId;
    }
}
