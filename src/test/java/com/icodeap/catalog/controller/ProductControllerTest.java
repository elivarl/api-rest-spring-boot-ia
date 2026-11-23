package com.icodeap.catalog.controller;

import com.icodeap.catalog.entity.Product;
import com.icodeap.catalog.exception.ResourceNotFoundException;
import com.icodeap.catalog.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void createProduct_whenValid_returns201WithBody() throws Exception {
        Product saved = Product.builder()
                .id(1L).name("Teclado").description("Mecánico").category("Periféricos")
                .stock(10).active(true)
                .build();
        when(productService.createProduct(any(Product.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Teclado",
                                  "description": "Mecánico",
                                  "category": "Periféricos",
                                  "stock": 10,
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Teclado"))
                .andExpect(jsonPath("$.description").value("Mecánico"))
                .andExpect(jsonPath("$.category").value("Periféricos"))
                .andExpect(jsonPath("$.stock").value(10))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createProduct_whenInvalid_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "category": "Periféricos",
                                  "stock": -1,
                                  "active": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Datos de entrada inválidos"))
                .andExpect(jsonPath("$.errors.name").value("El nombre es obligatorio"))
                .andExpect(jsonPath("$.errors.stock").value("El stock no puede ser negativo"));

        verifyNoInteractions(productService);
    }

    @Test
    void getProductById_whenNotFound_returns404() throws Exception {
        when(productService.getProductById(99L))
                .thenThrow(new ResourceNotFoundException("Producto no encontrado con el ID: 99"));

        mockMvc.perform(get("/api/v1/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Producto no encontrado con el ID: 99"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void getProductById_whenIdNotNumeric_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/products/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("El parámetro 'id' tiene un formato inválido"));

        verifyNoInteractions(productService);
    }

    @Test
    void getProductById_whenUnexpectedError_returns500WithoutInternalDetails() throws Exception {
        when(productService.getProductById(1L))
                .thenThrow(new RuntimeException("SQL: SELECT * FROM products WHERE id = 1"));

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Error interno del servidor"))
                .andExpect(content().string(not(containsString("SQL"))))
                .andExpect(content().string(not(containsString("products"))));
    }
}
