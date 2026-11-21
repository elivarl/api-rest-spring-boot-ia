package com.icodeap.catalog.dto;

import com.icodeap.catalog.entity.Product;

public record ProductResponse(
        Long id,
        String name,
        String description,
        String category,
        Integer stock,
        Boolean active
) {

    public static ProductResponse fromEntity(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getStock(),
                product.getActive()
        );
    }
}
