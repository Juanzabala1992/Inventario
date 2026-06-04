package com.technicaltest.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateInventoryRequest(@Valid @NotNull InventoryData data) {
    public record InventoryData(
            @NotBlank String type,
            @Valid @NotNull InventoryAttributes attributes
    ) {
    }

    public record InventoryAttributes(@NotNull @Min(0) Integer quantity) {
    }
}
