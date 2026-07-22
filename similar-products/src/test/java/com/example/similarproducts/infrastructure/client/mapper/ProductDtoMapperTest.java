package com.example.similarproducts.infrastructure.client.mapper;

import com.example.similarproducts.domain.model.Product;
import com.example.similarproducts.domain.model.ProductId;
import com.example.similarproducts.infrastructure.client.dto.ProductDetailDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductDtoMapperTest {

    private final ProductDtoMapper mapper = new ProductDtoMapper();

    @Test
    void flattensDeduplicatesAndPreservesOrder() {
        String[][] raw = {{"1", "2", "3"}, {"3", "4"}, null};
        List<ProductId> ids = mapper.toProductIds(raw);
        assertThat(ids).containsExactly(
                ProductId.of("1"), ProductId.of("2"), ProductId.of("3"), ProductId.of("4"));
    }

    @Test
    void dropsBlankIds() {
        String[][] raw = {{"1", "", "  ", "2"}};
        assertThat(mapper.toProductIds(raw))
                .containsExactly(ProductId.of("1"), ProductId.of("2"));
    }

    @Test
    void handlesNullPayload() {
        assertThat(mapper.toProductIds(null)).isEmpty();
    }

    @Test
    void mapsDetailPreservingNullFields() {
        Product p = mapper.toDomain(new ProductDetailDto("7", null, new BigDecimal("5.00"), null));
        assertThat(p.id()).isEqualTo(ProductId.of("7"));
        assertThat(p.name()).isNull();
        assertThat(p.price()).isEqualByComparingTo("5.00");
        assertThat(p.availability()).isNull();
    }
}
