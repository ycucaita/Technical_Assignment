package com.example.similarproducts.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * API response item. Null fields are omitted globally
 * (spring.jackson.default-property-inclusion=non_null), so a degraded product serialises
 * as simply {@code {"id":"..."}}.
 */
@Schema(description = "A similar product. Non-id fields are omitted when unknown.")
public record ProductResponse(
        @Schema(example = "1") String id,
        @Schema(example = "Shirt") String name,
        @Schema(example = "19.99") BigDecimal price,
        @Schema(example = "true") Boolean availability
) {
}
