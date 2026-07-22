package com.example.similarproducts.domain.model;

import java.math.BigDecimal;

/**
 * Product aggregate.
 * <p>
 * Fields other than the id are nullable on purpose: the API contract says that when a
 * detail cannot be resolved we still return the product with only its id populated, and
 * that null fields are omitted from the response. {@link #idOnly(ProductId)} builds that
 * degraded representation.
 */
public record Product(ProductId id, String name, BigDecimal price, Boolean availability) {

    public Product {
        if (id == null) {
            throw new IllegalArgumentException("product id must not be null");
        }
    }

    /** Fully populated product. */
    public static Product of(ProductId id, String name, BigDecimal price, Boolean availability) {
        return new Product(id, name, price, availability);
    }

    /** Degraded product used when the detail lookup fails or returns 404. */
    public static Product idOnly(ProductId id) {
        return new Product(id, null, null, null);
    }

    public boolean isIdOnly() {
        return name == null && price == null && availability == null;
    }
}
