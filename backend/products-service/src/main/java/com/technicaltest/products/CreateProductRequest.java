package com.technicaltest.products;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateProductRequest(@Valid @NotNull ProductData data) {
    public record ProductData(
            @NotBlank String type,
            @Valid @NotNull CreateProductAttributes attributes
    ) {
    }

    public record CreateProductAttributes(
            @NotBlank String name,
            @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal price,
            String description
    ) {
    }
}

