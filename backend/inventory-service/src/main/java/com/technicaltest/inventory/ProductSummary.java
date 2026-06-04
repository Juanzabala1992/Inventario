package com.technicaltest.inventory;

import java.math.BigDecimal;

public record ProductSummary(
        String id,
        String name,
        BigDecimal price,
        String description
) {
}

