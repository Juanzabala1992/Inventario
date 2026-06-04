package com.technicaltest.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePurchaseRequest(@Valid @NotNull PurchaseData data) {
    public record PurchaseData(
            @NotBlank String type,
            @Valid @NotNull PurchaseAttributes attributes
    ) {
    }

    public record PurchaseAttributes(
            @NotBlank String productId,
            @NotNull @Min(1) Integer quantity
    ) {
    }
}
