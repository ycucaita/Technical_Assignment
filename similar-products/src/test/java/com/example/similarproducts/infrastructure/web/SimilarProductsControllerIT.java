package com.example.similarproducts.infrastructure.web;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * End-to-end slice: real HTTP server + WebClient + resilience + cache, with the two upstream
 * endpoints faked by WireMock. Each test uses distinct product ids so the shared caches do not
 * leak state between tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SimilarProductsControllerIT {

    static final WireMockServer WIREMOCK = new WireMockServer(options().dynamicPort());

    static {
        WIREMOCK.start();
    }

    @Value("${local.server.port}")
    int port;

    WebTestClient webTestClient;

    @BeforeEach
    void buildClient() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(10))
                .build();
    }

    @DynamicPropertySource
    static void upstreamProperties(DynamicPropertyRegistry registry) {
        registry.add("product-api.base-url", () -> "http://localhost:" + WIREMOCK.port());
    }

    @AfterEach
    void resetStubs() {
        WIREMOCK.resetAll();
    }

    @AfterAll
    static void stopWireMock() {
        WIREMOCK.stop();
    }

    private void stubSimilarIds(String productId, String jsonBody) {
        WIREMOCK.stubFor(get(urlEqualTo("/product/" + productId + "/similarIds"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonBody)));
    }

    private void stubDetail(String productId, String jsonBody) {
        WIREMOCK.stubFor(get(urlEqualTo("/product/" + productId))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonBody)));
    }

    @Test
    void returnsAllSimilarProductDetails() {
        stubSimilarIds("1", "[[\"2\",\"3\"]]");
        stubDetail("2", "{\"id\":\"2\",\"name\":\"Shirt\",\"price\":19.99,\"availability\":true}");
        stubDetail("3", "{\"id\":\"3\",\"name\":\"Blazer\",\"price\":49.99,\"availability\":false}");

        webTestClient.get().uri("/product/1/similar")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].id").isEqualTo("2")
                .jsonPath("$[0].name").isEqualTo("Shirt")
                .jsonPath("$[0].availability").isEqualTo(true)
                .jsonPath("$[1].id").isEqualTo("3")
                .jsonPath("$[1].availability").isEqualTo(false);
    }

    @Test
    void missingDetailIsReturnedAsIdOnly() {
        stubSimilarIds("10", "[[\"11\",\"12\"]]");
        stubDetail("11", "{\"id\":\"11\",\"name\":\"Jacket\",\"price\":89.99,\"availability\":true}");
        WIREMOCK.stubFor(get(urlEqualTo("/product/12")).willReturn(aResponse().withStatus(404)));

        webTestClient.get().uri("/product/10/similar")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].id").isEqualTo("11")
                .jsonPath("$[0].name").isEqualTo("Jacket")
                .jsonPath("$[1].id").isEqualTo("12")
                .jsonPath("$[1].name").doesNotExist()
                .jsonPath("$[1].price").doesNotExist()
                .jsonPath("$[1].availability").doesNotExist();
    }

    @Test
    void unknownProductReturns404() {
        WIREMOCK.stubFor(get(urlEqualTo("/product/999/similarIds"))
                .willReturn(aResponse().withStatus(404)));

        webTestClient.get().uri("/product/999/similar")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Product not found");
    }

    @Test
    void slowDetailTimesOutAndDegradesToIdOnly() {
        stubSimilarIds("20", "[[\"21\"]]");
        WIREMOCK.stubFor(get(urlEqualTo("/product/21"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withFixedDelay(1000) // > call-timeout (300ms) -> pipeline timeout
                        .withBody("{\"id\":\"21\",\"name\":\"Late\",\"price\":1.0,\"availability\":true}")));

        webTestClient.get().uri("/product/20/similar")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].id").isEqualTo("21")
                .jsonPath("$[0].name").doesNotExist();
    }

    @Test
    void generatesACorrelationIdWhenNoneIsSupplied() {
        stubSimilarIds("40", "[[\"41\"]]");
        stubDetail("41", "{\"id\":\"41\",\"name\":\"Traced\",\"price\":2.0,\"availability\":true}");

        webTestClient.get().uri("/product/40/similar")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Correlation-Id");
    }

    @Test
    void echoesAnIncomingCorrelationId() {
        stubSimilarIds("50", "[[\"51\"]]");
        stubDetail("51", "{\"id\":\"51\",\"name\":\"Traced\",\"price\":2.0,\"availability\":true}");

        webTestClient.get().uri("/product/50/similar")
                .header("X-Correlation-Id", "trace-abc-123")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Correlation-Id", "trace-abc-123");
    }

    @Test
    void repeatedCallsAreServedFromCache() {
        stubSimilarIds("30", "[[\"31\"]]");
        stubDetail("31", "{\"id\":\"31\",\"name\":\"Cached\",\"price\":5.0,\"availability\":true}");

        for (int i = 0; i < 3; i++) {
            webTestClient.get().uri("/product/30/similar")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody().jsonPath("$[0].name").isEqualTo("Cached");
        }

        verify(exactly(1), getRequestedFor(urlEqualTo("/product/30/similarIds")));
        verify(exactly(1), getRequestedFor(urlEqualTo("/product/31")));
    }
}
