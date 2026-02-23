package com.alvaro.pricewise.repository;

import com.alvaro.pricewise.entity.Company;
import com.alvaro.pricewise.entity.Product;
import com.alvaro.pricewise.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ProductRepository Tests")
class ProductRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    private Company company1;
    private Company company2;
    private User user;

    @BeforeEach
    void setUp() {
        company1 = entityManager.persistAndFlush(Company.builder()
                .name("Empresa Test 1")
                .businessType("ecommerce")
                .build());

        company2 = entityManager.persistAndFlush(Company.builder()
                .name("Empresa Test 2")
                .businessType("retail")
                .build());

        user = entityManager.persistAndFlush(User.builder()
                .username("testuser")
                .email("test@email.com")
                .password("encoded")
                .company(company1)
                .role(User.Role.COMPANY_ADMIN)
                .build());
    }

    private Product createProduct(String name, String sku, String category, String brand,
                                  BigDecimal price, Company company, boolean active) {
        return entityManager.persistAndFlush(Product.builder()
                .name(name)
                .sku(sku)
                .currentPrice(price)
                .category(category)
                .brand(brand)
                .company(company)
                .createdBy(user)
                .active(active)
                .monitoringEnabled(true)
                .build());
    }

    @Nested
    @DisplayName("findByCompanyIdAndActiveTrue")
    class FindByCompanyAndActiveTests {

        @Test
        @DisplayName("Devuelve solo productos activos de la empresa")
        void shouldReturnOnlyActiveProducts() {
            createProduct("Activo 1", "SKU-A1", "Electro", "Sony", new BigDecimal("100"), company1, true);
            createProduct("Activo 2", "SKU-A2", "Electro", "Sony", new BigDecimal("200"), company1, true);
            createProduct("Inactivo", "SKU-I1", "Electro", "Sony", new BigDecimal("300"), company1, false);

            Page<Product> result = productRepository.findByCompanyIdAndActiveTrue(company1.getId(), PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).allMatch(Product::getActive);
        }

        @Test
        @DisplayName("No devuelve productos de otra empresa")
        void shouldNotReturnProductsFromOtherCompany() {
            createProduct("Producto C1", "SKU-C1", "Electro", "Sony", new BigDecimal("100"), company1, true);
            createProduct("Producto C2", "SKU-C2", "Electro", "Sony", new BigDecimal("200"), company2, true);

            List<Product> result = productRepository.findByCompanyIdAndActiveTrue(company1.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Producto C1");
        }
    }

    @Nested
    @DisplayName("findBySkuAndCompanyIdAndActiveTrue")
    class FindBySkuAndCompanyIdAndActiveTrueTests {

        @Test
        @DisplayName("findBySkuAndCompanyIdAndActiveTrue no encuentra producto inactivo (soft-deleted)")
        void findBySkuAndCompanyIdAndActiveTrue_inactiveProduct_returnsEmpty() {
            createProduct("Borrado", "SKU-DELETED", "Electro", "Sony", new BigDecimal("50"), company1, false);

            Optional<Product> found = productRepository.findBySkuAndCompanyIdAndActiveTrue("SKU-DELETED", company1.getId());

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("findBySkuAndCompanyIdAndActiveTrue encuentra producto activo")
        void findBySkuAndCompanyIdAndActiveTrue_activeProduct_returnsProduct() {
            createProduct("Activo", "SKU-ACTIVE", "Electro", "Sony", new BigDecimal("50"), company1, true);

            Optional<Product> found = productRepository.findBySkuAndCompanyIdAndActiveTrue("SKU-ACTIVE", company1.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Activo");
        }
    }

    @Nested
    @DisplayName("searchProducts (JPQL custom)")
    class SearchProductsTests {

        @Test
        @DisplayName("Busca por nombre (case insensitive)")
        void searchByName_caseInsensitive() {
            createProduct("iPhone 15 Pro", "SKU-IP", "Telefonia", "Apple", new BigDecimal("1199"), company1, true);
            createProduct("Samsung Galaxy", "SKU-SG", "Telefonia", "Samsung", new BigDecimal("999"), company1, true);

            Page<Product> result = productRepository.searchProducts(
                    company1.getId(), "iphone", null, null, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("iPhone 15 Pro");
        }

        @Test
        @DisplayName("Busca por categoria")
        void searchByCategory() {
            createProduct("TV LG", "SKU-TV", "Television", "LG", new BigDecimal("599"), company1, true);
            createProduct("iPhone", "SKU-IP", "Telefonia", "Apple", new BigDecimal("1199"), company1, true);

            Page<Product> result = productRepository.searchProducts(
                    company1.getId(), null, "Television", null, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getCategory()).isEqualTo("Television");
        }

        @Test
        @DisplayName("Busca por marca")
        void searchByBrand() {
            createProduct("AirPods", "SKU-AP", "Audio", "Apple", new BigDecimal("199"), company1, true);
            createProduct("WH-1000", "SKU-WH", "Audio", "Sony", new BigDecimal("299"), company1, true);

            Page<Product> result = productRepository.searchProducts(
                    company1.getId(), null, null, "Apple", PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getBrand()).isEqualTo("Apple");
        }

        @Test
        @DisplayName("Busca con todos los filtros null devuelve todos los activos")
        void searchWithAllNullFilters_returnsAllActive() {
            createProduct("P1", "SKU-1", "Cat1", "Brand1", new BigDecimal("100"), company1, true);
            createProduct("P2", "SKU-2", "Cat2", "Brand2", new BigDecimal("200"), company1, true);
            createProduct("Inactivo", "SKU-3", "Cat1", "Brand1", new BigDecimal("300"), company1, false);

            Page<Product> result = productRepository.searchProducts(
                    company1.getId(), null, null, null, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("No devuelve productos de otra empresa")
        void searchProducts_differentCompany_returnsEmpty() {
            createProduct("P1", "SKU-1", "Cat1", "Brand1", new BigDecimal("100"), company2, true);

            Page<Product> result = productRepository.searchProducts(
                    company1.getId(), null, null, null, PageRequest.of(0, 10));

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("countByCompanyId")
    class CountTests {

        @Test
        @DisplayName("Cuenta productos totales de la empresa")
        void countByCompanyId_returnsCorrectCount() {
            createProduct("P1", "SKU-1", null, null, new BigDecimal("100"), company1, true);
            createProduct("P2", "SKU-2", null, null, new BigDecimal("200"), company1, false);
            createProduct("P3", "SKU-3", null, null, new BigDecimal("300"), company2, true);

            long count = productRepository.countByCompanyId(company1.getId());

            assertThat(count).isEqualTo(2); // activos + inactivos de company1
        }

        @Test
        @DisplayName("Cuenta solo productos activos de la empresa")
        void countByCompanyIdAndActiveTrue_returnsOnlyActive() {
            createProduct("P1", "SKU-1", null, null, new BigDecimal("100"), company1, true);
            createProduct("P2", "SKU-2", null, null, new BigDecimal("200"), company1, false);

            long count = productRepository.countByCompanyIdAndActiveTrue(company1.getId());

            assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("findDistinctCategoriesByCompanyId / findDistinctBrandsByCompanyId")
    class DistinctQueriesTests {

        @Test
        @DisplayName("Devuelve categorias unicas de la empresa")
        void findDistinctCategories_returnsUnique() {
            createProduct("P1", "SKU-1", "Electro", "Sony", new BigDecimal("100"), company1, true);
            createProduct("P2", "SKU-2", "Electro", "LG", new BigDecimal("200"), company1, true);
            createProduct("P3", "SKU-3", "Telefonia", "Apple", new BigDecimal("300"), company1, true);
            createProduct("P4", "SKU-4", "Hogar", "Bosch", new BigDecimal("400"), company2, true);

            List<String> categories = productRepository.findDistinctCategoriesByCompanyId(company1.getId());

            assertThat(categories).hasSize(2);
            assertThat(categories).containsExactlyInAnyOrder("Electro", "Telefonia");
        }

        @Test
        @DisplayName("Excluye categorias null")
        void findDistinctCategories_excludesNull() {
            createProduct("P1", "SKU-1", null, "Sony", new BigDecimal("100"), company1, true);
            createProduct("P2", "SKU-2", "Electro", "Sony", new BigDecimal("200"), company1, true);

            List<String> categories = productRepository.findDistinctCategoriesByCompanyId(company1.getId());

            assertThat(categories).hasSize(1);
            assertThat(categories).contains("Electro");
        }

        @Test
        @DisplayName("Devuelve marcas unicas de la empresa")
        void findDistinctBrands_returnsUnique() {
            createProduct("P1", "SKU-1", "Electro", "Sony", new BigDecimal("100"), company1, true);
            createProduct("P2", "SKU-2", "Electro", "Sony", new BigDecimal("200"), company1, true);
            createProduct("P3", "SKU-3", "Electro", "Samsung", new BigDecimal("300"), company1, true);

            List<String> brands = productRepository.findDistinctBrandsByCompanyId(company1.getId());

            assertThat(brands).hasSize(2);
            assertThat(brands).containsExactlyInAnyOrder("Sony", "Samsung");
        }
    }

}
