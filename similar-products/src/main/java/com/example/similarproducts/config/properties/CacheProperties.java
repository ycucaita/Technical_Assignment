package com.example.similarproducts.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Per-cache sizing and TTL.
 */
@ConfigurationProperties(prefix = "cache")
public record CacheProperties(CacheSpec similarIds, CacheSpec productDetail) {

    public record CacheSpec(long maximumSize, Duration expireAfterWrite) {
    }
}
