package com.technicaltest.products;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String productId) {
        super("Product " + productId + " does not exist.");
    }
}

