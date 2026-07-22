package com.example.similarproducts.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI similarProductsOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Similar Products API")
                .version("1.0.0")
                .description("Aggregates similar-product details from two upstream services, "
                        + "with circuit breaking, retries, bulkheads, timeouts and caching.")
                .license(new License().name("MIT")));
    }
}
