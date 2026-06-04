package com.technicaltest.inventory;

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

    public static Map<String, Object> productResource(ProductSummary product) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("name", product.name());
        attributes.put("price", product.price());
        attributes.put("description", product.description());

        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("type", "products");
        resource.put("id", product.id());
        resource.put("attributes", attributes);
        return resource;
    }

    public static Map<String, Object> inventoryResource(InventoryItem inventoryItem) {
        Map<String, Object> relationshipData = Map.of(
                "type", "products",
                "id", inventoryItem.getProductId()
        );

        Map<String, Object> relationships = Map.of(
                "product", Map.of("data", relationshipData)
        );

        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("type", "inventories");
        resource.put("id", inventoryItem.getProductId());
        resource.put("attributes", Map.of("quantity", inventoryItem.getQuantity()));
        resource.put("relationships", relationships);
        return resource;
    }

    public static Map<String, Object> purchaseResource(Purchase purchase) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("productId", purchase.getProductId());
        attributes.put("quantity", purchase.getQuantity());
        attributes.put("unitPrice", purchase.getUnitPrice());
        attributes.put("total", purchase.getTotal());
        attributes.put("remainingQuantity", purchase.getRemainingQuantity());
        attributes.put("createdAt", purchase.getCreatedAt());

        Map<String, Object> relationships = Map.of(
                "product",
                Map.of("data", Map.of("type", "products", "id", purchase.getProductId()))
        );

        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("type", "purchases");
        resource.put("id", purchase.getId());
        resource.put("attributes", attributes);
        resource.put("relationships", relationships);
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

