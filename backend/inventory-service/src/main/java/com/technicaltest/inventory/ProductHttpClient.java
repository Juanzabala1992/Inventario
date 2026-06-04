package com.technicaltest.inventory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class ProductHttpClient implements ProductClient {
    private final String baseUrl;
    private final String apiKey;
    private final int timeoutMs;
    private final int retries;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ProductHttpClient(
            @Value("${products-service.url}") String baseUrl,
            @Value("${products-service.api-key}") String apiKey,
            @Value("${products-service.timeout-ms}") int timeoutMs,
            @Value("${products-service.retries}") int retries,
            ObjectMapper objectMapper
    ) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.timeoutMs = timeoutMs;
        this.retries = retries;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    @Override
    public ProductSummary getProduct(String productId) {
        String encodedProductId = URLEncoder.encode(productId, StandardCharsets.UTF_8);
        URI uri = URI.create(baseUrl.replaceAll("/$", "") + "/products/" + encodedProductId);

        for (int attempt = 0; attempt <= retries; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofMillis(timeoutMs))
                        .header("Accept", JsonApi.MEDIA_TYPE)
                        .header("x-api-key", apiKey)
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() == 404) {
                    throw new ProductNotFoundException(productId);
                }

                if (response.statusCode() >= 500 && attempt < retries) {
                    sleepBeforeRetry(attempt);
                    continue;
                }

                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new ProductServiceUnavailableException(
                            "Product service responded with HTTP " + response.statusCode() + "."
                    );
                }

                return mapProduct(response.body());
            } catch (ProductNotFoundException | ProductServiceUnavailableException exception) {
                throw exception;
            } catch (IOException | InterruptedException exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }

                if (attempt < retries) {
                    sleepBeforeRetry(attempt);
                    continue;
                }

                throw new ProductServiceUnavailableException("Product service request failed.");
            }
        }

        throw new ProductServiceUnavailableException("Product service request failed.");
    }

    private ProductSummary mapProduct(String responseBody) throws IOException {
        JsonNode data = objectMapper.readTree(responseBody).path("data");
        JsonNode attributes = data.path("attributes");

        if (!"products".equals(data.path("type").asText())
                || data.path("id").isMissingNode()
                || attributes.path("name").isMissingNode()
                || attributes.path("price").isMissingNode()) {
            throw new ProductServiceUnavailableException(
                    "Product service returned an invalid JSON API document."
            );
        }

        JsonNode descriptionNode = attributes.path("description");
        String description = descriptionNode.isMissingNode() || descriptionNode.isNull()
                ? null
                : descriptionNode.asText();

        return new ProductSummary(
                data.path("id").asText(),
                attributes.path("name").asText(),
                new BigDecimal(attributes.path("price").asText()),
                description
        );
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(50L * (attempt + 1));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}

