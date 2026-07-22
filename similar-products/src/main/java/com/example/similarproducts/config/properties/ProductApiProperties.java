package com.example.similarproducts.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Transport + fan-out settings for the outbound product API.
 */
@ConfigurationProperties(prefix = "product-api")
public record ProductApiProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration responseTimeout,
        Duration callTimeout,
        int maxConnections,
        Duration pendingAcquireTimeout,
        int concurrency
) {
}
