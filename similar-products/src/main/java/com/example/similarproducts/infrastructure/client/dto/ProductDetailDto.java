package com.example.similarproducts.infrastructure.client.dto;

import java.math.BigDecimal;

/**
 * Wire model for GET /product/{id}. Wrapper types are used so that any field the upstream
 * omits or sends as null stays null (and is therefore dropped from our response).
 */
public record ProductDetailDto(String id, String name, BigDecimal price, Boolean availability) {
}
