package com.icodeap.catalog.service.impl;

import com.icodeap.catalog.entity.Product;
import com.icodeap.catalog.exception.ResourceNotFoundException;
import com.icodeap.catalog.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void getProductById_whenNotFound_throwsResourceNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Producto no encontrado con el ID: 99");
    }

    @Test
    void updateProduct_whenExists_copiesFieldsAndKeepsId() {
        Product existing = Product.builder()
                .id(1L).name("Teclado").description("Mecánico").category("Periféricos")
                .stock(10).active(true)
                .build();
        Product details = Product.builder()
                .id(999L).name("Teclado inalámbrico").description("Bluetooth").category("Accesorios")
                .stock(3).active(false)
                .build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product updated = productService.updateProduct(1L, details);

        assertThat(updated.getId()).isEqualTo(1L);
        assertThat(updated.getName()).isEqualTo("Teclado inalámbrico");
        assertThat(updated.getDescription()).isEqualTo("Bluetooth");
        assertThat(updated.getCategory()).isEqualTo("Accesorios");
        assertThat(updated.getStock()).isEqualTo(3);
        assertThat(updated.getActive()).isFalse();
        verify(productRepository).save(existing);
    }

    @Test
    void deleteProduct_whenNotFound_doesNotDelete() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).delete(any(Product.class));
        verify(productRepository, never()).deleteById(any());
    }
}
