package com.example.similarproducts.config;

import com.example.similarproducts.application.service.GetSimilarProductsService;
import com.example.similarproducts.config.properties.ProductApiProperties;
import com.example.similarproducts.application.port.in.GetSimilarProductsUseCase;
import com.example.similarproducts.application.port.out.ProductDetailProvider;
import com.example.similarproducts.application.port.out.SimilarProductIdsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the framework-agnostic application service to its driven ports. Keeping this in the
 * config layer means the application/domain packages never import Spring.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public GetSimilarProductsUseCase getSimilarProductsUseCase(SimilarProductIdsProvider idsProvider,
                                                               ProductDetailProvider detailProvider,
                                                               ProductApiProperties props) {
        return new GetSimilarProductsService(idsProvider, detailProvider, props.concurrency());
    }
}
