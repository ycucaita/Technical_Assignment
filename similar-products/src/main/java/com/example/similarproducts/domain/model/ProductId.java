package com.example.similarproducts.domain.model;

/**
 * Value object for a product identifier.
 * Guarantees a non-blank, trimmed id so no other layer has to defend against blank ids.
 */
public record ProductId(String value) {

    public ProductId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("productId must not be blank");
        }
        value = value.trim();
    }

    public static ProductId of(String value) {
        return new ProductId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
