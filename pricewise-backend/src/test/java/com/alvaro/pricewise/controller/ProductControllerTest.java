package com.alvaro.pricewise.controller;

import com.alvaro.pricewise.dto.common.PageResponse;
import com.alvaro.pricewise.dto.product.ProductDTOs.*;
import com.alvaro.pricewise.exception.GlobalExceptionHandler;
import com.alvaro.pricewise.exception.ResourceNotFoundException;
import com.alvaro.pricewise.security.JwtService;
import com.alvaro.pricewise.security.UserPrincipal;
import com.alvaro.pricewise.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("ProductController Tests")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    private static final String BASE_URL = "/api/products";

    @BeforeEach
    void setAuth() {
        UserPrincipal principal = UserPrincipal.builder()
                .id(1L)
                .companyId(1L)
                .email("user@email.com")
                .username("user")
                .password("encoded")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_COMPANY_ADMIN")))
                .active(true)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private ProductResponse buildProductResponse() {
        return ProductResponse.builder()
                .id(1L)
                .name("iPhone 15")
                .sku("SKU-001")
                .currentPrice(new BigDecimal("999.99"))
                .category("Electrónica")
                .brand("Apple")
                .active(true)
                .monitoringEnabled(true)
                .stockQuantity(50)
                .build();
    }

    @Nested
    @DisplayName("POST /api/products")
    class CreateProductTests {

        @Test
        @DisplayName("Crear producto exitoso devuelve 201")
        void createProduct_Success_Returns201() throws Exception {
            CreateProductRequest request = CreateProductRequest.builder()
                    .name("iPhone 15")
                    .currentPrice(new BigDecimal("999.99"))
                    .costPrice(new BigDecimal("750.00"))
                    .sku("SKU-001")
                    .category("Electrónica")
                    .brand("Apple")
                    .build();

            when(productService.createProduct(eq(1L), eq(1L), any(CreateProductRequest.class)))
                    .thenReturn(buildProductResponse());

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("iPhone 15"))
                    .andExpect(jsonPath("$.data.sku").value("SKU-001"))
                    .andExpect(jsonPath("$.message").value("Producto creado exitosamente"));
        }

        @Test
        @DisplayName("Crear producto sin nombre devuelve 400")
        void createProduct_MissingName_Returns400() throws Exception {
            CreateProductRequest request = CreateProductRequest.builder()
                    .currentPrice(new BigDecimal("10.00"))
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Crear producto sin precio devuelve 400")
        void createProduct_MissingPrice_Returns400() throws Exception {
            CreateProductRequest request = CreateProductRequest.builder()
                    .name("Producto sin precio")
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Crear producto con precio 0 devuelve 400")
        void createProduct_ZeroPrice_Returns400() throws Exception {
            CreateProductRequest request = CreateProductRequest.builder()
                    .name("Producto gratis")
                    .currentPrice(BigDecimal.ZERO)
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Crear producto sin precio de coste devuelve 400")
        void createProduct_MissingCostPrice_Returns400() throws Exception {
            CreateProductRequest request = CreateProductRequest.builder()
                    .name("Producto sin coste")
                    .currentPrice(new BigDecimal("29.99"))
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/products/{id}")
    class GetProductTests {

        @Test
        @DisplayName("Obtener producto existente devuelve 200")
        void getProduct_Exists_Returns200() throws Exception {
            when(productService.getProduct(eq(1L), eq(1L))).thenReturn(buildProductResponse());

            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("iPhone 15"));
        }

        @Test
        @DisplayName("Obtener producto inexistente devuelve 404")
        void getProduct_NotFound_Returns404() throws Exception {
            when(productService.getProduct(eq(1L), eq(999L)))
                    .thenThrow(new ResourceNotFoundException("Producto no encontrado"));

            mockMvc.perform(get(BASE_URL + "/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Producto no encontrado"));
        }
    }

    @Nested
    @DisplayName("GET /api/products")
    class ListProductsTests {

        @Test
        @DisplayName("Listar productos devuelve pagina con datos")
        void getProducts_ReturnsPaginatedList() throws Exception {
            ProductListResponse item = ProductListResponse.builder()
                    .id(1L).name("iPhone 15").sku("SKU-001")
                    .currentPrice(new BigDecimal("999.99"))
                    .category("Electrónica").brand("Apple")
                    .build();

            PageResponse<ProductListResponse> pageResponse = PageResponse.<ProductListResponse>builder()
                    .content(List.of(item))
                    .pageNumber(0).pageSize(20)
                    .totalElements(1).totalPages(1)
                    .first(true).last(true)
                    .build();

            when(productService.getProducts(eq(1L), any())).thenReturn(pageResponse);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].name").value("iPhone 15"))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        @DisplayName("Listar productos vacio devuelve pagina sin datos")
        void getProducts_Empty_ReturnsEmptyPage() throws Exception {
            PageResponse<ProductListResponse> emptyPage = PageResponse.<ProductListResponse>builder()
                    .content(List.of())
                    .pageNumber(0).pageSize(20)
                    .totalElements(0).totalPages(0)
                    .first(true).last(true)
                    .build();

            when(productService.getProducts(eq(1L), any())).thenReturn(emptyPage);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(0))
                    .andExpect(jsonPath("$.data.content").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/products/search")
    class SearchProductsTests {

        @Test
        @DisplayName("Buscar por nombre devuelve resultados")
        void searchProducts_ByName_ReturnsResults() throws Exception {
            ProductListResponse item = ProductListResponse.builder()
                    .id(1L).name("iPhone 15").build();

            PageResponse<ProductListResponse> pageResponse = PageResponse.<ProductListResponse>builder()
                    .content(List.of(item))
                    .totalElements(1).totalPages(1)
                    .build();

            when(productService.searchProducts(eq(1L), eq("iPhone"), any(), any(), any()))
                    .thenReturn(pageResponse);

            mockMvc.perform(get(BASE_URL + "/search").param("name", "iPhone"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].name").value("iPhone 15"));
        }

        @Test
        @DisplayName("Buscar sin filtros devuelve resultados")
        void searchProducts_NoFilters_ReturnsResults() throws Exception {
            PageResponse<ProductListResponse> pageResponse = PageResponse.<ProductListResponse>builder()
                    .content(List.of())
                    .totalElements(0).totalPages(0)
                    .build();

            when(productService.searchProducts(eq(1L), any(), any(), any(), any()))
                    .thenReturn(pageResponse);

            mockMvc.perform(get(BASE_URL + "/search"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("PUT /api/products/{id}")
    class UpdateProductTests {

        @Test
        @DisplayName("Actualizar producto exitoso devuelve 200")
        void updateProduct_Success_Returns200() throws Exception {
            UpdateProductRequest request = UpdateProductRequest.builder()
                    .name("iPhone 15 Pro")
                    .currentPrice(new BigDecimal("1199.99"))
                    .build();

            ProductResponse updated = ProductResponse.builder()
                    .id(1L).name("iPhone 15 Pro")
                    .currentPrice(new BigDecimal("1199.99"))
                    .build();

            when(productService.updateProduct(eq(1L), eq(1L), any(UpdateProductRequest.class)))
                    .thenReturn(updated);

            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("iPhone 15 Pro"))
                    .andExpect(jsonPath("$.message").value("Producto actualizado"));
        }

        @Test
        @DisplayName("Actualizar producto inexistente devuelve 404")
        void updateProduct_NotFound_Returns404() throws Exception {
            UpdateProductRequest request = UpdateProductRequest.builder()
                    .name("Nuevo nombre")
                    .build();

            when(productService.updateProduct(eq(1L), eq(999L), any(UpdateProductRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Producto no encontrado"));

            mockMvc.perform(put(BASE_URL + "/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("DELETE /api/products/{id}")
    class DeleteProductTests {

        @Test
        @DisplayName("Eliminar producto exitoso devuelve 200")
        void deleteProduct_Success_Returns200() throws Exception {
            doNothing().when(productService).deleteProduct(eq(1L), eq(1L));

            mockMvc.perform(delete(BASE_URL + "/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Producto eliminado"));
        }

        @Test
        @DisplayName("Eliminar producto inexistente devuelve 404")
        void deleteProduct_NotFound_Returns404() throws Exception {
            doThrow(new ResourceNotFoundException("Producto no encontrado"))
                    .when(productService).deleteProduct(eq(1L), eq(999L));

            mockMvc.perform(delete(BASE_URL + "/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/products/categories & /brands & /count")
    class MetadataTests {

        @Test
        @DisplayName("Obtener categorias devuelve lista")
        void getCategories_ReturnsList() throws Exception {
            when(productService.getCategories(eq(1L)))
                    .thenReturn(List.of("Electrónica", "Hogar"));

            mockMvc.perform(get(BASE_URL + "/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0]").value("Electrónica"))
                    .andExpect(jsonPath("$.data[1]").value("Hogar"));
        }

        @Test
        @DisplayName("Obtener marcas devuelve lista")
        void getBrands_ReturnsList() throws Exception {
            when(productService.getBrands(eq(1L)))
                    .thenReturn(List.of("Apple", "Samsung"));

            mockMvc.perform(get(BASE_URL + "/brands"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0]").value("Apple"))
                    .andExpect(jsonPath("$.data[1]").value("Samsung"));
        }

        @Test
        @DisplayName("Contar productos devuelve total")
        void countProducts_ReturnsCount() throws Exception {
            when(productService.countProducts(eq(1L))).thenReturn(42L);

            mockMvc.perform(get(BASE_URL + "/count"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(42));
        }
    }
}
