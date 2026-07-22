package com.example.similarproducts.infrastructure.web;

import com.example.similarproducts.domain.model.ProductId;
import com.example.similarproducts.application.port.in.GetSimilarProductsUseCase;
import com.example.similarproducts.infrastructure.web.dto.ProductResponse;
import com.example.similarproducts.infrastructure.web.mapper.ProductResponseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Inbound REST adapter. Streams the aggregated similar products as they resolve.
 */
@RestController
@RequestMapping("/product")
@Validated
@Tag(name = "Similar Products", description = "Aggregates similar-product details from upstream services")
public class SimilarProductsController {

    private final GetSimilarProductsUseCase useCase;
    private final ProductResponseMapper mapper;

    public SimilarProductsController(GetSimilarProductsUseCase useCase, ProductResponseMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @GetMapping("/{productId}/similar")
    @Operation(summary = "Get similar products",
            description = "Returns the detail of every product similar to the given one. "
                    + "Products whose detail cannot be resolved are returned with only their id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Similar products (possibly partially degraded)"),
            @ApiResponse(responseCode = "404", description = "The requested product does not exist"),
            @ApiResponse(responseCode = "503", description = "Upstream unavailable (circuit open / timeout)")
    })
    public Flux<ProductResponse> getSimilarProducts(@PathVariable @NotBlank String productId) {
        return useCase.getSimilarProducts(ProductId.of(productId))
                .map(mapper::toResponse);
    }
}
