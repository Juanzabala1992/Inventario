package com.technicaltest.inventory;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping(produces = JsonApi.MEDIA_TYPE)
public class InventoryController {
    private final InventoryService inventoryService;
    private final ProductClient productClient;
    private final PurchaseRepository purchaseRepository;

    public InventoryController(
            InventoryService inventoryService,
            ProductClient productClient,
            PurchaseRepository purchaseRepository
    ) {
        this.inventoryService = inventoryService;
        this.productClient = productClient;
        this.purchaseRepository = purchaseRepository;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok()
                .contentType(JsonApi.JSON_API_MEDIA_TYPE)
                .body(JsonApi.document(JsonApi.healthResource("inventory-service")));
    }

    @GetMapping("/inventory/{productId}")
    public ResponseEntity<Map<String, Object>> getInventory(@PathVariable String productId) {
        ProductSummary product = productClient.getProduct(productId);
        InventoryItem inventoryItem = inventoryService.getOrZero(productId);

        return ResponseEntity.ok()
                .contentType(JsonApi.JSON_API_MEDIA_TYPE)
                .body(JsonApi.document(
                        JsonApi.inventoryResource(inventoryItem),
                        List.of(JsonApi.productResource(product))
                ));
    }

    @PutMapping(path = "/inventory/{productId}", consumes = {JsonApi.MEDIA_TYPE, "application/json"})
    public ResponseEntity<Map<String, Object>> updateInventory(
            @PathVariable String productId,
            @Valid @RequestBody UpdateInventoryRequest request
    ) {
        if (!"inventories".equals(request.data().type())) {
            throw new IllegalArgumentException("Request body must contain data.type = inventories.");
        }

        ProductSummary product = productClient.getProduct(productId);
        InventoryItem inventoryItem = inventoryService.setQuantity(
                productId,
                request.data().attributes().quantity()
        );

        return ResponseEntity.ok()
                .contentType(JsonApi.JSON_API_MEDIA_TYPE)
                .body(JsonApi.document(
                        JsonApi.inventoryResource(inventoryItem),
                        List.of(JsonApi.productResource(product))
                ));
    }

    @PostMapping(path = "/purchases", consumes = {JsonApi.MEDIA_TYPE, "application/json"})
    public ResponseEntity<Map<String, Object>> createPurchase(
            @Valid @RequestBody CreatePurchaseRequest request
    ) {
        if (!"purchases".equals(request.data().type())) {
            throw new IllegalArgumentException("Request body must contain data.type = purchases.");
        }

        ProductSummary product = productClient.getProduct(request.data().attributes().productId());
        Purchase purchase = inventoryService.purchase(product, request.data().attributes().quantity());

        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(JsonApi.JSON_API_MEDIA_TYPE)
                .body(JsonApi.document(
                        JsonApi.purchaseResource(purchase),
                        List.of(JsonApi.productResource(product))
                ));
    }

    @GetMapping("/purchases")
    public ResponseEntity<Map<String, Object>> listPurchases() {
        var purchases = purchaseRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(JsonApi::purchaseResource)
                .toList();

        return ResponseEntity.ok()
                .contentType(JsonApi.JSON_API_MEDIA_TYPE)
                .body(JsonApi.document(purchases));
    }
}

