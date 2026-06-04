package com.technicaltest.products;

import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonApi {
    public static final String MEDIA_TYPE = "application/vnd.api+json";
    public static final MediaType JSON_API_MEDIA_TYPE = MediaType.valueOf(MEDIA_TYPE);

    private JsonApi() {
    }

    public static Map<String, Object> document(Object data) {
        return new LinkedHashMap<>(Map.of("data", data));
    }

    public static Map<String, Object> document(Object data, List<Object> included) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("data", data);
        document.put("included", included);
        return document;
    }

    public static Map<String, Object> productResource(Product product) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("name", product.getName());
        attributes.put("price", product.getPrice());
        attributes.put("description", product.getDescription());

        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("type", "products");
        resource.put("id", product.getId());
        resource.put("attributes", attributes);
        return resource;
    }

    public static Map<String, Object> healthResource(String serviceName) {
        return Map.of(
                "type", "health",
                "id", serviceName,
                "attributes", Map.of("status", "ok")
        );
    }

    public static Map<String, Object> errorDocument(int status, String title, String detail) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("status", String.valueOf(status));
        error.put("title", title);
        error.put("detail", detail);
        return Map.of("errors", new ArrayList<>(List.of(error)));
    }
}

