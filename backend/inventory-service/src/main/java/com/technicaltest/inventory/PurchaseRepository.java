package com.technicaltest.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, String> {
    List<Purchase> findAllByOrderByCreatedAtDesc();
}

