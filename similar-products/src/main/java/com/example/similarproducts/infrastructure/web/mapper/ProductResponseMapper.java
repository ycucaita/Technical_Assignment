package com.example.similarproducts.infrastructure.web.mapper;

import com.example.similarproducts.domain.model.Product;
import com.example.similarproducts.infrastructure.web.dto.ProductResponse;
import org.springframework.stereotype.Component;

@Component
public class ProductResponseMapper {

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.id().value(),
                product.name(),
                product.price(),
                product.availability());
    }
}
