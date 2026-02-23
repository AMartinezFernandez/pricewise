package com.alvaro.pricewise.repository;

import com.alvaro.pricewise.entity.*;
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
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("CompetitorPriceRepository Integration Tests")
class CompetitorPriceRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CompetitorPriceRepository competitorPriceRepository;

    private Company company;
    private User user;
    private Product product1;
    private Product product2;
    private Competitor competitor;

    @BeforeEach
    void setUp() {
        company = entityManager.persistAndFlush(Company.builder()
                .name("Empresa Test")
                .businessType("ecommerce")
                .build());

        user = entityManager.persistAndFlush(User.builder()
                .username("testuser")
                .email("test@email.com")
                .password("encoded")
                .company(company)
                .role(User.Role.COMPANY_ADMIN)
                .build());

        product1 = entityManager.persistAndFlush(Product.builder()
                .name("Producto 1")
                .sku("SKU-1")
                .currentPrice(new BigDecimal("100.00"))
                .company(company)
                .createdBy(user)
                .active(true)
                .monitoringEnabled(true)
                .build());

        product2 = entityManager.persistAndFlush(Product.builder()
                .name("Producto 2")
                .sku("SKU-2")
                .currentPrice(new BigDecimal("200.00"))
                .company(company)
                .createdBy(user)
                .active(true)
                .monitoringEnabled(true)
                .build());

        competitor = entityManager.persistAndFlush(Competitor.builder()
                .name("Amazon ES")
                .code("AMAZON_ES")
                .baseUrl("https://www.amazon.es")
                .sourceType(Competitor.SourceType.API)
                .active(true)
                .build());
    }

    private CompetitorPrice createCompetitorPrice(Product product, BigDecimal price,
                                                   LocalDateTime scrapedAt, boolean available) {
        return entityManager.persistAndFlush(CompetitorPrice.builder()
                .product(product)
                .competitor(competitor)
                .price(price)
                .currency("EUR")
                .available(available)
                .scrapedAt(scrapedAt)
                .source("keepa-api")
                .build());
    }

    @Nested
    @DisplayName("findTopByProductIdOrderByScrapedAtDesc")
    class FindLatestByProductIdTests {

        @Test
        @DisplayName("Devuelve el precio más reciente del producto")
        void shouldReturnMostRecentPrice() {
            LocalDateTime now = LocalDateTime.now();
            createCompetitorPrice(product1, new BigDecimal("89.99"), now.minusDays(5), true);
            createCompetitorPrice(product1, new BigDecimal("92.50"), now.minusDays(2), true);
            createCompetitorPrice(product1, new BigDecimal("95.00"), now.minusHours(1), true);

            Optional<CompetitorPrice> result = competitorPriceRepository
                    .findTopByProductIdOrderByScrapedAtDesc(product1.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getPrice()).isEqualByComparingTo(new BigDecimal("95.00"));
        }

        @Test
        @DisplayName("Devuelve vacío si no hay precios del producto")
        void shouldReturnEmptyWhenNoPrices() {
            Optional<CompetitorPrice> result = competitorPriceRepository
                    .findTopByProductIdOrderByScrapedAtDesc(product1.getId());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("No mezcla precios de distintos productos")
        void shouldNotMixProductPrices() {
            LocalDateTime now = LocalDateTime.now();
            createCompetitorPrice(product1, new BigDecimal("50.00"), now.minusDays(1), true);
            createCompetitorPrice(product2, new BigDecimal("999.00"), now, true);

            Optional<CompetitorPrice> result = competitorPriceRepository
                    .findTopByProductIdOrderByScrapedAtDesc(product1.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getPrice()).isEqualByComparingTo(new BigDecimal("50.00"));
        }
    }

    @Nested
    @DisplayName("findByProductIdOrderByScrapedAtDesc (paginado)")
    class FindByProductIdPaginatedTests {

        @Test
        @DisplayName("Pagina correctamente precios del producto")
        void shouldPaginatePrices() {
            LocalDateTime now = LocalDateTime.now();
            for (int i = 0; i < 12; i++) {
                createCompetitorPrice(product1, new BigDecimal(80 + i), now.minusHours(12 - i), true);
            }

            Page<CompetitorPrice> page0 = competitorPriceRepository
                    .findByProductIdOrderByScrapedAtDesc(product1.getId(), PageRequest.of(0, 5));

            assertThat(page0.getContent()).hasSize(5);
            assertThat(page0.getTotalElements()).isEqualTo(12);
            assertThat(page0.getTotalPages()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("deleteByScrapedAtBefore (TTL)")
    class DeleteTTLTests {

        @Test
        @DisplayName("Elimina precios anteriores a la fecha dada")
        void shouldDeleteOldPrices() {
            LocalDateTime now = LocalDateTime.now();
            createCompetitorPrice(product1, new BigDecimal("80.00"), now.minusDays(400), true);
            createCompetitorPrice(product1, new BigDecimal("85.00"), now.minusDays(10), true);

            competitorPriceRepository.deleteByScrapedAtBefore(now.minusDays(365));
            entityManager.flush();

            Page<CompetitorPrice> remaining = competitorPriceRepository
                    .findByProductIdOrderByScrapedAtDesc(product1.getId(), PageRequest.of(0, 100));
            assertThat(remaining.getContent()).hasSize(1);
            assertThat(remaining.getContent().get(0).getPrice()).isEqualByComparingTo(new BigDecimal("85.00"));
        }
    }

    @Nested
    @DisplayName("countScrapedToday")
    class CountScrapedTodayTests {

        @Test
        @DisplayName("Cuenta precios capturados hoy")
        void shouldCountTodaysPrices() {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
            createCompetitorPrice(product1, new BigDecimal("80.00"), todayStart.plusHours(1), true);
            createCompetitorPrice(product1, new BigDecimal("85.00"), todayStart.plusHours(2), true);
            createCompetitorPrice(product2, new BigDecimal("180.00"), todayStart.minusDays(2), true);

            long count = competitorPriceRepository.countScrapedToday(todayStart);

            assertThat(count).isEqualTo(2);
        }
    }

}
