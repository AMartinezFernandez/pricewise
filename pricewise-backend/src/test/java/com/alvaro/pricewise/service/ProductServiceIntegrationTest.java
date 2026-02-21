package com.alvaro.pricewise.service;

import com.alvaro.pricewise.dto.product.ProductDTOs.CreateProductRequest;
import com.alvaro.pricewise.dto.product.ProductDTOs.ProductResponse;
import com.alvaro.pricewise.dto.product.ProductDTOs.UpdateProductRequest;
import com.alvaro.pricewise.entity.Company;
import com.alvaro.pricewise.entity.PriceHistory;
import com.alvaro.pricewise.entity.Product;
import com.alvaro.pricewise.entity.User;
import com.alvaro.pricewise.repository.PriceHistoryRepository;
import com.alvaro.pricewise.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("ProductService Integration Tests")
class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PriceHistoryRepository priceHistoryRepository;

    @Autowired
    private EntityManager entityManager;

    @MockBean
    private KeepaService keepaService;

    private Company company;
    private User user;

    @BeforeEach
    void setUp() {
        company = Company.builder()
                .name("Empresa Integration Test")
                .businessType("ecommerce")
                .build();
        entityManager.persist(company);

        user = User.builder()
                .username("intuser")
                .email("int@test.com")
                .password("encoded")
                .company(company)
                .role(User.Role.COMPANY_ADMIN)
                .build();
        entityManager.persist(user);
        entityManager.flush();
    }

    @Nested
    @DisplayName("Crear producto → PriceHistory INITIAL")
    class CreateProductTests {

        @Test
        @DisplayName("Al crear producto se genera PriceHistory con tipo INITIAL")
        void shouldCreateInitialPriceHistory() {
            CreateProductRequest request = CreateProductRequest.builder()
                    .name("Producto Integración")
                    .asin("B012345678")
                    .currentPrice(new BigDecimal("49.99"))
                    .category("Test")
                    .brand("TestBrand")
                    .build();

            ProductResponse response = productService.createProduct(company.getId(), user.getId(), request);
            entityManager.flush();

            List<PriceHistory> history = priceHistoryRepository
                    .findByProductIdOrderByRecordedAtDesc(response.getId());

            assertThat(history).hasSize(1);
            assertThat(history.get(0).getChangeType()).isEqualTo(PriceHistory.ChangeType.INITIAL);
            assertThat(history.get(0).getPrice()).isEqualByComparingTo(new BigDecimal("49.99"));
            assertThat(history.get(0).getPreviousPrice()).isNull();
            assertThat(history.get(0).getChangeReason()).isEqualTo("Precio inicial");
        }

        @Test
        @DisplayName("ASIN se almacena en campos sku y asin")
        void shouldStoreAsinInBothFields() {
            CreateProductRequest request = CreateProductRequest.builder()
                    .name("Producto ASIN")
                    .asin("B098765432")
                    .currentPrice(new BigDecimal("29.99"))
                    .build();

            ProductResponse response = productService.createProduct(company.getId(), user.getId(), request);
            entityManager.flush();

            Product product = productRepository.findById(response.getId()).orElseThrow();
            assertThat(product.getSku()).isEqualTo("B098765432");
            assertThat(product.getAsin()).isEqualTo("B098765432");
        }
    }

    @Nested
    @DisplayName("Actualizar precio → PriceHistory INCREASE/DECREASE")
    class UpdatePriceTests {

        @Test
        @DisplayName("Subida de precio genera PriceHistory INCREASE")
        void shouldCreateIncreaseHistory() {
            CreateProductRequest createReq = CreateProductRequest.builder()
                    .name("Producto Precio")
                    .asin("B111111111")
                    .currentPrice(new BigDecimal("100.00"))
                    .build();
            ProductResponse created = productService.createProduct(company.getId(), user.getId(), createReq);
            entityManager.flush();

            UpdateProductRequest updateReq = UpdateProductRequest.builder()
                    .currentPrice(new BigDecimal("120.00"))
                    .build();
            productService.updateProduct(company.getId(), created.getId(), updateReq);
            entityManager.flush();

            List<PriceHistory> history = priceHistoryRepository
                    .findByProductIdOrderByRecordedAtDesc(created.getId());

            assertThat(history).hasSize(2);
            // Most recent first
            PriceHistory increase = history.get(0);
            assertThat(increase.getChangeType()).isEqualTo(PriceHistory.ChangeType.INCREASE);
            assertThat(increase.getPrice()).isEqualByComparingTo(new BigDecimal("120.00"));
            assertThat(increase.getPreviousPrice()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("Bajada de precio genera PriceHistory DECREASE")
        void shouldCreateDecreaseHistory() {
            CreateProductRequest createReq = CreateProductRequest.builder()
                    .name("Producto Bajada")
                    .asin("B222222222")
                    .currentPrice(new BigDecimal("100.00"))
                    .build();
            ProductResponse created = productService.createProduct(company.getId(), user.getId(), createReq);
            entityManager.flush();

            UpdateProductRequest updateReq = UpdateProductRequest.builder()
                    .currentPrice(new BigDecimal("80.00"))
                    .build();
            productService.updateProduct(company.getId(), created.getId(), updateReq);
            entityManager.flush();

            List<PriceHistory> history = priceHistoryRepository
                    .findByProductIdOrderByRecordedAtDesc(created.getId());

            assertThat(history).hasSize(2);
            PriceHistory decrease = history.get(0);
            assertThat(decrease.getChangeType()).isEqualTo(PriceHistory.ChangeType.DECREASE);
            assertThat(decrease.getPrice()).isEqualByComparingTo(new BigDecimal("80.00"));
            assertThat(decrease.getPreviousPrice()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("Mismo precio no genera nuevo PriceHistory")
        void shouldNotCreateHistoryWhenPriceUnchanged() {
            CreateProductRequest createReq = CreateProductRequest.builder()
                    .name("Producto Sin Cambio")
                    .asin("B333333333")
                    .currentPrice(new BigDecimal("100.00"))
                    .build();
            ProductResponse created = productService.createProduct(company.getId(), user.getId(), createReq);
            entityManager.flush();

            UpdateProductRequest updateReq = UpdateProductRequest.builder()
                    .currentPrice(new BigDecimal("100.00"))
                    .build();
            productService.updateProduct(company.getId(), created.getId(), updateReq);
            entityManager.flush();

            List<PriceHistory> history = priceHistoryRepository
                    .findByProductIdOrderByRecordedAtDesc(created.getId());

            assertThat(history).hasSize(1); // Only INITIAL
        }
    }

    @Nested
    @DisplayName("Soft-delete → producto desaparece de queries")
    class SoftDeleteTests {

        @Test
        @DisplayName("Producto eliminado no aparece en listado activos")
        void shouldNotAppearInActiveProducts() {
            CreateProductRequest request = CreateProductRequest.builder()
                    .name("Producto Eliminar")
                    .asin("B444444444")
                    .currentPrice(new BigDecimal("50.00"))
                    .build();
            ProductResponse created = productService.createProduct(company.getId(), user.getId(), request);
            entityManager.flush();

            long countBefore = productService.countProducts(company.getId());

            productService.deleteProduct(company.getId(), created.getId());
            entityManager.flush();

            long countAfter = productService.countProducts(company.getId());

            assertThat(countAfter).isEqualTo(countBefore - 1);
        }

        @Test
        @DisplayName("Producto eliminado mantiene active=false en BD")
        void shouldSetActiveToFalse() {
            CreateProductRequest request = CreateProductRequest.builder()
                    .name("Producto SoftDel")
                    .asin("B555555555")
                    .currentPrice(new BigDecimal("75.00"))
                    .build();
            ProductResponse created = productService.createProduct(company.getId(), user.getId(), request);
            entityManager.flush();

            productService.deleteProduct(company.getId(), created.getId());
            entityManager.flush();
            entityManager.clear();

            Product deleted = productRepository.findById(created.getId()).orElseThrow();
            assertThat(deleted.getActive()).isFalse();
        }

        @Test
        @DisplayName("Se puede recrear producto con mismo ASIN tras soft-delete")
        void shouldAllowRecreatingDeletedProduct() {
            CreateProductRequest request = CreateProductRequest.builder()
                    .name("Producto Original")
                    .asin("B666666666")
                    .currentPrice(new BigDecimal("60.00"))
                    .build();
            ProductResponse created = productService.createProduct(company.getId(), user.getId(), request);
            productService.deleteProduct(company.getId(), created.getId());
            entityManager.flush();

            // Re-create with same ASIN should work
            CreateProductRequest recreate = CreateProductRequest.builder()
                    .name("Producto Recreado")
                    .asin("B666666666")
                    .currentPrice(new BigDecimal("65.00"))
                    .build();
            ProductResponse recreated = productService.createProduct(company.getId(), user.getId(), recreate);

            assertThat(recreated.getId()).isNotEqualTo(created.getId());
            assertThat(recreated.getName()).isEqualTo("Producto Recreado");
        }
    }
}
