package com.technicaltest.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "purchases")
public class Purchase {
    @Id
    private String id;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(nullable = false)
    private int remainingQuantity;

    @Column(nullable = false)
    private Instant createdAt;

    protected Purchase() {
    }

    public Purchase(ProductSummary product, int quantity, int remainingQuantity) {
        this.id = UUID.randomUUID().toString();
        this.productId = product.id();
        this.quantity = quantity;
        this.unitPrice = product.price();
        this.total = product.price().multiply(BigDecimal.valueOf(quantity));
        this.remainingQuantity = remainingQuantity;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public int getRemainingQuantity() {
        return remainingQuantity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

