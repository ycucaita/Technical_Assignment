package com.example.similarproducts;

import com.example.similarproducts.infrastructure.logging.MdcCorrelationIdAccessor;
import io.micrometer.context.ContextRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import reactor.core.publisher.Hooks;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SimilarProductsApplication {

    public static void main(String[] args) {
        // Make thread-locals (our MDC correlation id, plus Micrometer observation scopes) follow
        // the reactive signal across thread boundaries, so log tracing survives the Netty/scheduler
        // thread hops inherent to WebFlux. Must be set before the reactive pipelines are built.
        Hooks.enableAutomaticContextPropagation();
        ContextRegistry.getInstance().registerThreadLocalAccessor(new MdcCorrelationIdAccessor());

        SpringApplication.run(SimilarProductsApplication.class, args);
    }
}
