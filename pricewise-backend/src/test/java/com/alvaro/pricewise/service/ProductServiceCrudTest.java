package com.alvaro.pricewise.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.alvaro.pricewise.dto.product.ProductDTOs.CreateProductRequest;
import com.alvaro.pricewise.dto.product.ProductDTOs.ProductResponse;
import com.alvaro.pricewise.dto.product.ProductDTOs.UpdateProductRequest;
import com.alvaro.pricewise.entity.Company;
import com.alvaro.pricewise.entity.PriceHistory;
import com.alvaro.pricewise.entity.Product;
import com.alvaro.pricewise.entity.User;
import com.alvaro.pricewise.exception.BadRequestException;
import com.alvaro.pricewise.exception.ResourceNotFoundException;
import com.alvaro.pricewise.repository.CompanyRepository;
import com.alvaro.pricewise.repository.CompetitorPriceRepository;
import com.alvaro.pricewise.repository.PriceHistoryRepository;
import com.alvaro.pricewise.repository.ProductRepository;
import com.alvaro.pricewise.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService - CRUD operations")
class ProductServiceCrudTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private PriceHistoryRepository priceHistoryRepository;
    @Mock
    private CompetitorPriceRepository competitorPriceRepository;
    @Mock
    private KeepaService keepaService;

    private ProductService productService;

    private Company company;
    private User user;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, userRepository, companyRepository, priceHistoryRepository, competitorPriceRepository, keepaService);

        company = Company.builder()
                .id(1L)
                .name("Test Company")
                .build();

        user = User.builder()
                .id(10L)
                .username("testuser")
                .email("test@example.com")
                .password("encoded-password")
                .company(company)
                .role(User.Role.EMPLOYEE)
                .active(true)
                .build();
    }

    // ---------------------------------------------------------------
    // createProduct
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("createProduct")
    class CreateProduct {

        @Test
        @DisplayName("should create product successfully and record initial price history")
        void createProduct_Success() {
            // Arrange
            CreateProductRequest request = CreateProductRequest.builder()
                    .name("New Product")
                    .sku("SKU-001")
                    .currentPrice(new BigDecimal("29.99"))
                    .costPrice(new BigDecimal("15.00"))
                    .category("Electronics")
                    .brand("TestBrand")
                    .monitoringEnabled(true)
                    .stockQuantity(50)
                    .build();

            when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
            when(userRepository.findById(10L)).thenReturn(Optional.of(user));
            when(productRepository.findBySkuAndCompanyIdAndActiveTrue("SKU-001", 1L)).thenReturn(Optional.empty());
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
                Product p = invocation.getArgument(0);
                p.setId(100L);
                return p;
            });
            when(priceHistoryRepository.save(any(PriceHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            ProductResponse response = productService.createProduct(1L, 10L, request);

            // Assert
            assertNotNull(response);
            assertEquals(100L, response.getId());
            assertEquals("New Product", response.getName());
            assertEquals("SKU-001", response.getSku());
            assertEquals(new BigDecimal("29.99"), response.getCurrentPrice());
            assertEquals("Electronics", response.getCategory());
            assertEquals("TestBrand", response.getBrand());
            assertEquals(true, response.getActive());

            verify(productRepository).save(any(Product.class));
            verify(priceHistoryRepository).save(argThat(ph ->
                    ph.getChangeType() == PriceHistory.ChangeType.INITIAL
                    && ph.getPreviousPrice() == null
                    && ph.getPrice().compareTo(new BigDecimal("29.99")) == 0
            ));
        }

        @Test
        @DisplayName("should throw BadRequestException when SKU is duplicated")
        void createProduct_DuplicateSku_ThrowsBadRequest() {
            // Arrange
            CreateProductRequest request = CreateProductRequest.builder()
                    .name("Another Product")
                    .sku("DUPLICATE-SKU")
                    .currentPrice(new BigDecimal("10.00"))
                    .build();

            Product existing = Product.builder().id(99L).sku("DUPLICATE-SKU").build();

            when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
            when(userRepository.findById(10L)).thenReturn(Optional.of(user));
            when(productRepository.findBySkuAndCompanyIdAndActiveTrue("DUPLICATE-SKU", 1L)).thenReturn(Optional.of(existing));

            // Act & Assert
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> productService.createProduct(1L, 10L, request));

            assertEquals("Ya existe un producto con el ASIN: DUPLICATE-SKU", ex.getMessage());
            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when company not found")
        void createProduct_CompanyNotFound_ThrowsNotFound() {
            // Arrange
            CreateProductRequest request = CreateProductRequest.builder()
                    .name("Product X")
                    .currentPrice(new BigDecimal("5.00"))
                    .build();

            when(companyRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> productService.createProduct(999L, 10L, request));

            assertEquals("Empresa no encontrada", ex.getMessage());
            verify(productRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------
    // getProduct
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("getProduct")
    class GetProduct {

        @Test
        @DisplayName("should return product when found and belongs to company")
        void getProduct_Success() {
            // Arrange
            Product product = Product.builder()
                    .id(1L)
                    .name("Test Product")
                    .sku("SKU-100")
                    .currentPrice(new BigDecimal("19.99"))
                    .costPrice(new BigDecimal("10.00"))
                    .category("Books")
                    .brand("Publisher")
                    .active(true)
                    .monitoringEnabled(true)
                    .stockQuantity(5)
                    .company(company)
                    .createdBy(user)
                    .build();

            when(productRepository.findByCompanyIdAndIdWithCreatedBy(1L, 1L)).thenReturn(Optional.of(product));

            // Act
            ProductResponse response = productService.getProduct(1L, 1L);

            // Assert
            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals("Test Product", response.getName());
            assertEquals("SKU-100", response.getSku());
            assertEquals("testuser", response.getCreatedByUsername());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when product does not exist")
        void getProduct_NotFound_ThrowsNotFound() {
            // Arrange
            when(productRepository.findByCompanyIdAndIdWithCreatedBy(1L, 999L)).thenReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> productService.getProduct(1L, 999L));

            assertEquals("Producto no encontrado", ex.getMessage());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when product belongs to a different company")
        void getProduct_WrongCompany_ThrowsNotFound() {
            // Arrange: query by companyId=1 won't find product of companyId=2
            when(productRepository.findByCompanyIdAndIdWithCreatedBy(1L, 5L)).thenReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> productService.getProduct(1L, 5L));

            assertEquals("Producto no encontrado", ex.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // updateProduct
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("updateProduct")
    class UpdateProduct {

        @Test
        @DisplayName("should update product and create price history when price changes")
        void updateProduct_Success_PriceChanged() {
            // Arrange
            Product product = Product.builder()
                    .id(1L)
                    .name("Old Name")
                    .sku("SKU-UPD")
                    .currentPrice(new BigDecimal("20.00"))
                    .costPrice(new BigDecimal("10.00"))
                    .active(true)
                    .monitoringEnabled(true)
                    .stockQuantity(10)
                    .company(company)
                    .createdBy(user)
                    .build();

            UpdateProductRequest request = UpdateProductRequest.builder()
                    .name("Updated Name")
                    .currentPrice(new BigDecimal("25.00"))
                    .build();

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(priceHistoryRepository.save(any(PriceHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            ProductResponse response = productService.updateProduct(1L, 1L, request);

            // Assert
            assertEquals("Updated Name", response.getName());
            assertEquals(new BigDecimal("25.00"), response.getCurrentPrice());

            verify(priceHistoryRepository).save(argThat(ph ->
                    ph.getChangeType() == PriceHistory.ChangeType.INCREASE
                    && ph.getPreviousPrice().compareTo(new BigDecimal("20.00")) == 0
                    && ph.getPrice().compareTo(new BigDecimal("25.00")) == 0
            ));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when product does not exist")
        void updateProduct_NotFound_ThrowsNotFound() {
            // Arrange
            UpdateProductRequest request = UpdateProductRequest.builder()
                    .name("Does not matter")
                    .build();

            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> productService.updateProduct(1L, 999L, request));

            assertEquals("Producto no encontrado", ex.getMessage());
            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw BadRequestException when SKU belongs to a different product")
        void updateProduct_DuplicateSkuOnDifferentProduct_ThrowsBadRequest() {
            // Arrange
            Product product = Product.builder()
                    .id(1L)
                    .name("My Product")
                    .sku("ORIGINAL-SKU")
                    .currentPrice(new BigDecimal("30.00"))
                    .active(true)
                    .company(company)
                    .createdBy(user)
                    .build();

            Product conflicting = Product.builder()
                    .id(2L)
                    .sku("TAKEN-SKU")
                    .build();

            UpdateProductRequest request = UpdateProductRequest.builder()
                    .sku("TAKEN-SKU")
                    .build();

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productRepository.findBySkuAndCompanyIdAndActiveTrue("TAKEN-SKU", 1L)).thenReturn(Optional.of(conflicting));

            // Act & Assert
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> productService.updateProduct(1L, 1L, request));

            assertEquals("Ya existe otro producto con el ASIN: TAKEN-SKU", ex.getMessage());
            verify(productRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------
    // deleteProduct
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("deleteProduct")
    class DeleteProduct {

        @Test
        @DisplayName("should soft-delete product by setting active to false")
        void deleteProduct_Success() {
            // Arrange
            Product product = Product.builder()
                    .id(1L)
                    .name("To Delete")
                    .currentPrice(new BigDecimal("10.00"))
                    .active(true)
                    .company(company)
                    .build();

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            productService.deleteProduct(1L, 1L);

            // Assert
            assertFalse(product.getActive());
            verify(productRepository).save(argThat(p -> !p.getActive()));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when product does not exist")
        void deleteProduct_NotFound_ThrowsNotFound() {
            // Arrange
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> productService.deleteProduct(1L, 999L));

            assertEquals("Producto no encontrado", ex.getMessage());
            verify(productRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------
    // getCategories / getBrands / countProducts
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Catalog queries")
    class CatalogQueries {

        @Test
        @DisplayName("getCategories should return distinct categories for a company")
        void getCategories_ReturnsList() {
            // Arrange
            List<String> categories = List.of("Electronics", "Books", "Clothing");
            when(productRepository.findDistinctCategoriesByCompanyId(1L)).thenReturn(categories);

            // Act
            List<String> result = productService.getCategories(1L);

            // Assert
            assertEquals(3, result.size());
            assertEquals("Electronics", result.get(0));
            assertEquals("Books", result.get(1));
            assertEquals("Clothing", result.get(2));
            verify(productRepository).findDistinctCategoriesByCompanyId(1L);
        }

        @Test
        @DisplayName("getBrands should return distinct brands for a company")
        void getBrands_ReturnsList() {
            // Arrange
            List<String> brands = List.of("Apple", "Samsung");
            when(productRepository.findDistinctBrandsByCompanyId(1L)).thenReturn(brands);

            // Act
            List<String> result = productService.getBrands(1L);

            // Assert
            assertEquals(2, result.size());
            assertEquals("Apple", result.get(0));
            assertEquals("Samsung", result.get(1));
            verify(productRepository).findDistinctBrandsByCompanyId(1L);
        }

        @Test
        @DisplayName("countProducts should return count of active products for a company")
        void countProducts_ReturnsCount() {
            // Arrange
            when(productRepository.countByCompanyIdAndActiveTrue(1L)).thenReturn(42L);

            // Act
            long count = productService.countProducts(1L);

            // Assert
            assertEquals(42L, count);
            verify(productRepository).countByCompanyIdAndActiveTrue(1L);
        }
    }
}
