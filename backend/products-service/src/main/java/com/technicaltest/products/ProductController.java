package com.technicaltest.products;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping(produces = JsonApi.MEDIA_TYPE)
public class ProductController {
    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok()
                .contentType(JsonApi.JSON_API_MEDIA_TYPE)
                .body(JsonApi.document(JsonApi.healthResource("products-service")));
    }

    @PostMapping(path = "/products", consumes = {JsonApi.MEDIA_TYPE, "application/json"})
    public ResponseEntity<Map<String, Object>> createProduct(
            @Valid @RequestBody CreateProductRequest request
    ) {
        if (!"products".equals(request.data().type())) {
            throw new IllegalArgumentException("Request body must contain data.type = products.");
        }

        Product product = productRepository.save(new Product(
                request.data().attributes().name().trim(),
                request.data().attributes().price(),
                request.data().attributes().description()
        ));

        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(JsonApi.JSON_API_MEDIA_TYPE)
                .body(JsonApi.document(JsonApi.productResource(product)));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<Map<String, Object>> getProduct(@PathVariable String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        return ResponseEntity.ok()
                .contentType(JsonApi.JSON_API_MEDIA_TYPE)
                .body(JsonApi.document(JsonApi.productResource(product)));
    }

    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> listProducts() {
        var products = productRepository.findAll().stream()
                .sorted(Comparator.comparing(Product::getCreatedAt).reversed())
                .map(JsonApi::productResource)
                .toList();

        return ResponseEntity.ok()
                .contentType(JsonApi.JSON_API_MEDIA_TYPE)
                .body(JsonApi.document(products));
    }
}

