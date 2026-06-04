package com.technicaltest.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductHttpClientTest {
    private HttpServer server;
    private String baseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void getsProductUsingApiKey() {
        server.createContext("/products/product-1", exchange -> {
            assertThat(exchange.getRequestHeaders().getFirst("x-api-key")).isEqualTo("test-key");
            writeJson(exchange, 200, """
                    {
                      "data": {
                        "type": "products",
                        "id": "product-1",
                        "attributes": {
                          "name": "Keyboard",
                          "price": 100,
                          "description": "Mechanical keyboard"
                        }
                      }
                    }
                    """);
        });

        ProductHttpClient client = new ProductHttpClient(
                baseUrl,
                "test-key",
                500,
                0,
                objectMapper
        );

        ProductSummary product = client.getProduct("product-1");

        assertThat(product.id()).isEqualTo("product-1");
        assertThat(product.name()).isEqualTo("Keyboard");
        assertThat(product.description()).isEqualTo("Mechanical keyboard");
    }

    @Test
    void retriesTemporaryProductServiceFailures() {
        AtomicInteger attempts = new AtomicInteger();
        server.createContext("/products/product-1", exchange -> {
            if (attempts.incrementAndGet() == 1) {
                writeJson(exchange, 503, """
                        {"errors":[{"status":"503","title":"Unavailable","detail":"Temporary"}]}
                        """);
                return;
            }

            writeJson(exchange, 200, """
                    {
                      "data": {
                        "type": "products",
                        "id": "product-1",
                        "attributes": {
                          "name": "Keyboard",
                          "price": 100,
                          "description": null
                        }
                      }
                    }
                    """);
        });

        ProductHttpClient client = new ProductHttpClient(
                baseUrl,
                "test-key",
                500,
                1,
                objectMapper
        );

        ProductSummary product = client.getProduct("product-1");

        assertThat(product.id()).isEqualTo("product-1");
        assertThat(product.description()).isNull();
        assertThat(attempts).hasValue(2);
    }

    @Test
    void mapsNotFoundResponseToDomainException() {
        server.createContext("/products/missing", exchange -> writeJson(exchange, 404, """
                {"errors":[{"status":"404","title":"Not found","detail":"Missing"}]}
                """));

        ProductHttpClient client = new ProductHttpClient(
                baseUrl,
                "test-key",
                500,
                0,
                objectMapper
        );

        assertThatThrownBy(() -> client.getProduct("missing"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void rejectsInvalidJsonApiDocuments() {
        server.createContext("/products/broken", exchange -> writeJson(exchange, 200, """
                {
                  "data": {
                    "type": "products",
                    "id": "broken",
                    "attributes": {
                      "name": "Broken product"
                    }
                  }
                }
                """));

        ProductHttpClient client = new ProductHttpClient(
                baseUrl,
                "test-key",
                500,
                0,
                objectMapper
        );

        assertThatThrownBy(() -> client.getProduct("broken"))
                .isInstanceOf(ProductServiceUnavailableException.class);
    }

    @Test
    void mapsUnauthorizedResponseToIntegrationException() {
        server.createContext("/products/product-1", exchange -> writeJson(exchange, 401, """
                {"errors":[{"status":"401","title":"Unauthorized","detail":"Invalid key"}]}
                """));

        ProductHttpClient client = new ProductHttpClient(
                baseUrl,
                "wrong-key",
                500,
                0,
                objectMapper
        );

        assertThatThrownBy(() -> client.getProduct("product-1"))
                .isInstanceOf(ProductServiceUnavailableException.class);
    }

    private static void writeJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", JsonApi.MEDIA_TYPE);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}

