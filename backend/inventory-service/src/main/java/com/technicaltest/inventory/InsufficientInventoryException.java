package com.technicaltest.inventory;

public class InsufficientInventoryException extends RuntimeException {
    public InsufficientInventoryException(String productId, int requested, int available) {
        super("Product " + productId + " has " + available
                + " units available, but " + requested + " were requested.");
    }
}

