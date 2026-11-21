package com.icodeap.catalog.dto;

import com.icodeap.catalog.entity.Product;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
        String name,

        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        String description,

        @NotBlank(message = "La categoría es obligatoria")
        @Size(max = 60, message = "La categoría no puede superar los 60 caracteres")
        String category,

        @NotNull(message = "El stock es obligatorio")
        @PositiveOrZero(message = "El stock no puede ser negativo")
        Integer stock,

        @NotNull(message = "El estado activo es obligatorio")
        Boolean active
) {

    public Product toEntity() {
        return Product.builder()
                .name(name)
                .description(description)
                .category(category)
                .stock(stock)
                .active(active)
                .build();
    }
}
