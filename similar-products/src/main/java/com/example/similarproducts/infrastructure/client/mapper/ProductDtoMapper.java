package com.example.similarproducts.infrastructure.client.mapper;

import com.example.similarproducts.domain.model.Product;
import com.example.similarproducts.domain.model.ProductId;
import com.example.similarproducts.infrastructure.client.dto.ProductDetailDto;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Maps upstream wire models to domain types.
 */
@Component
public class ProductDtoMapper {

    public Product toDomain(ProductDetailDto dto) {
        return Product.of(ProductId.of(dto.id()), dto.name(), dto.price(), dto.availability());
    }

    /**
     * The similar-ids endpoint returns an array-of-arrays per the provided contract
     * (e.g. {@code [["1","2","3"]]}). We flatten it, drop blanks, and de-duplicate while
     * preserving first-seen order.
     *
     * <p>NOTE: if your real upstream returns a flat array ({@code ["1","2","3"]}) instead,
     * change the client's body type to {@code String[]} and simplify this method accordingly.
     */
    public List<ProductId> toProductIds(String[][] raw) {
        if (raw == null) {
            return List.of();
        }
        return Arrays.stream(raw)
                .filter(inner -> inner != null)
                .flatMap(Arrays::stream)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .map(ProductId::of)
                .toList();
    }
}
