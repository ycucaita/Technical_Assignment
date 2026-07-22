package com.example.similarproducts.application.service;

import com.example.similarproducts.domain.exception.ProductNotFoundException;
import com.example.similarproducts.domain.model.Product;
import com.example.similarproducts.domain.model.ProductId;
import com.example.similarproducts.application.port.out.ProductDetailProvider;
import com.example.similarproducts.application.port.out.SimilarProductIdsProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetSimilarProductsServiceTest {

    @Mock
    SimilarProductIdsProvider idsProvider;
    @Mock
    ProductDetailProvider detailProvider;

    private GetSimilarProductsService service() {
        return new GetSimilarProductsService(idsProvider, detailProvider, 10);
    }

    private static Product full(String id) {
        return Product.of(ProductId.of(id), "name-" + id, new BigDecimal("9.99"), true);
    }

    @Test
    void returnsAllDetailsPreservingUpstreamOrder() {
        when(idsProvider.getSimilarIds(eq(ProductId.of("1"))))
                .thenReturn(Mono.just(List.of(ProductId.of("2"), ProductId.of("3"), ProductId.of("4"))));
        when(detailProvider.getById(eq(ProductId.of("2")))).thenReturn(Mono.just(full("2")));
        when(detailProvider.getById(eq(ProductId.of("3")))).thenReturn(Mono.just(full("3")));
        when(detailProvider.getById(eq(ProductId.of("4")))).thenReturn(Mono.just(full("4")));

        StepVerifier.create(service().getSimilarProducts(ProductId.of("1")))
                .expectNext(full("2"), full("3"), full("4"))
                .verifyComplete();
    }

    @Test
    void degradesFailedDetailToIdOnlyButKeepsTheRest() {
        when(idsProvider.getSimilarIds(eq(ProductId.of("1"))))
                .thenReturn(Mono.just(List.of(ProductId.of("2"), ProductId.of("3"))));
        when(detailProvider.getById(eq(ProductId.of("2")))).thenReturn(Mono.just(full("2")));
        when(detailProvider.getById(eq(ProductId.of("3"))))
                .thenReturn(Mono.error(new ProductNotFoundException("3")));

        StepVerifier.create(service().getSimilarProducts(ProductId.of("1")))
                .expectNext(full("2"))
                .expectNext(Product.idOnly(ProductId.of("3")))
                .verifyComplete();
    }

    @Test
    void degradesOnTransientDetailErrorToo() {
        when(idsProvider.getSimilarIds(eq(ProductId.of("1"))))
                .thenReturn(Mono.just(List.of(ProductId.of("2"))));
        when(detailProvider.getById(eq(ProductId.of("2"))))
                .thenReturn(Mono.error(new RuntimeException("boom")));

        StepVerifier.create(service().getSimilarProducts(ProductId.of("1")))
                .expectNext(Product.idOnly(ProductId.of("2")))
                .verifyComplete();
    }

    @Test
    void emptyIdListYieldsEmptyResult() {
        when(idsProvider.getSimilarIds(eq(ProductId.of("1")))).thenReturn(Mono.just(List.of()));

        StepVerifier.create(service().getSimilarProducts(ProductId.of("1")))
                .verifyComplete();
    }

    @Test
    void failureToObtainIdListPropagates() {
        when(idsProvider.getSimilarIds(eq(ProductId.of("1"))))
                .thenReturn(Mono.error(new ProductNotFoundException("1")));

        StepVerifier.create(service().getSimilarProducts(ProductId.of("1")))
                .expectError(ProductNotFoundException.class)
                .verify();
    }
}
