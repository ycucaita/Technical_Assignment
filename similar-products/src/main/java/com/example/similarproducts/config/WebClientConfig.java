package com.example.similarproducts.config;

import com.example.similarproducts.config.properties.ProductApiProperties;
import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

/**
 * Tuned, pooled, non-blocking HTTP client for the outbound product API.
 * Transport timeouts here are the first line of defence; the resilience pipeline adds a hard
 * per-call ceiling on top.
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient productApiWebClient( ProductApiProperties props) {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("product-api")
                .maxConnections(props.maxConnections())
                .pendingAcquireTimeout(props.pendingAcquireTimeout())
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) props.connectTimeout().toMillis())
                .responseTimeout(props.responseTimeout());

        return  WebClient.builder()
                .baseUrl(props.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
