package com.technicaltest.inventory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final PurchaseRepository purchaseRepository;

    public InventoryService(
            InventoryRepository inventoryRepository,
            PurchaseRepository purchaseRepository
    ) {
        this.inventoryRepository = inventoryRepository;
        this.purchaseRepository = purchaseRepository;
    }

    @Transactional(readOnly = true)
    public InventoryItem getOrZero(String productId) {
        return inventoryRepository.findById(productId)
                .orElseGet(() -> new InventoryItem(productId, 0));
    }

    @Transactional
    public InventoryItem setQuantity(String productId, int quantity) {
        InventoryItem inventoryItem = inventoryRepository.findById(productId)
                .orElseGet(() -> new InventoryItem(productId, quantity));
        inventoryItem.setQuantity(quantity);
        return inventoryRepository.save(inventoryItem);
    }

    @Transactional
    public Purchase purchase(ProductSummary product, int quantity) {
        InventoryItem inventoryItem = inventoryRepository.findById(product.id())
                .orElseGet(() -> new InventoryItem(product.id(), 0));

        int available = inventoryItem.getQuantity();
        if (available < quantity) {
            throw new InsufficientInventoryException(product.id(), quantity, available);
        }

        int remainingQuantity = available - quantity;
        inventoryItem.setQuantity(remainingQuantity);
        inventoryRepository.save(inventoryItem);

        Purchase purchase = new Purchase(product, quantity, remainingQuantity);
        return purchaseRepository.save(purchase);
    }
}

