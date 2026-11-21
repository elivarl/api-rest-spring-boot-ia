package com.icodeap.catalog.repository;

import com.icodeap.catalog.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
