package com.example.similarproducts.config;

import com.example.similarproducts.config.properties.CacheProperties;
import com.example.similarproducts.domain.model.Product;
import com.example.similarproducts.domain.model.ProductId;
import com.example.similarproducts.infrastructure.cache.ReactiveCache;
import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CacheConfig {

    @Bean
    public ReactiveCache<List<ProductId>> similarIdsCache(CacheProperties props) {
        CacheProperties.CacheSpec spec = props.similarIds();
        AsyncCache<String, List<ProductId>> cache = Caffeine.newBuilder()
                .maximumSize(spec.maximumSize())
                .expireAfterWrite(spec.expireAfterWrite())
                .recordStats()
                .buildAsync();
        return new ReactiveCache<>(cache);
    }

    @Bean
    public ReactiveCache<Product> productDetailCache(CacheProperties props) {
        CacheProperties.CacheSpec spec = props.productDetail();
        AsyncCache<String, Product> cache = Caffeine.newBuilder()
                .maximumSize(spec.maximumSize())
                .expireAfterWrite(spec.expireAfterWrite())
                .recordStats()
                .buildAsync();
        return new ReactiveCache<>(cache);
    }
}
