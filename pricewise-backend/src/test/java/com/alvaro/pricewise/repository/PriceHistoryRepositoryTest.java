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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("PriceHistoryRepository Integration Tests")
class PriceHistoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PriceHistoryRepository priceHistoryRepository;

    private Company company;
    private User user;
    private Product product1;
    private Product product2;

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
    }

    private PriceHistory createPriceHistory(Product product, BigDecimal price,
                                             BigDecimal previousPrice,
                                             PriceHistory.ChangeType changeType,
                                             LocalDateTime recordedAt) {
        PriceHistory ph = PriceHistory.builder()
                .product(product)
                .price(price)
                .previousPrice(previousPrice)
                .changeType(changeType)
                .changeReason("Test")
                .build();
        PriceHistory persisted = entityManager.persistAndFlush(ph);
        // Override recordedAt for date-range tests (CreationTimestamp sets it auto)
        if (recordedAt != null) {
            entityManager.getEntityManager()
                    .createNativeQuery("UPDATE price_history SET recorded_at = ?1 WHERE id = ?2")
                    .setParameter(1, recordedAt)
                    .setParameter(2, persisted.getId())
                    .executeUpdate();
            entityManager.clear();
        }
        return persisted;
    }

    @Nested
    @DisplayName("findByProductIdOrderByRecordedAtDesc")
    class FindByProductTests {

        @Test
        @DisplayName("Devuelve historial del producto ordenado por fecha descendente")
        void shouldReturnHistoryOrderedByDate() {
            createPriceHistory(product1, new BigDecimal("100.00"), null,
                    PriceHistory.ChangeType.INITIAL, LocalDateTime.now().minusDays(3));
            createPriceHistory(product1, new BigDecimal("110.00"), new BigDecimal("100.00"),
                    PriceHistory.ChangeType.INCREASE, LocalDateTime.now().minusDays(2));
            createPriceHistory(product1, new BigDecimal("105.00"), new BigDecimal("110.00"),
                    PriceHistory.ChangeType.DECREASE, LocalDateTime.now().minusDays(1));

            List<PriceHistory> result = priceHistoryRepository.findByProductIdOrderByRecordedAtDesc(product1.getId());

            assertThat(result).hasSize(3);
            // Most recent first
            assertThat(result.get(0).getChangeType()).isEqualTo(PriceHistory.ChangeType.DECREASE);
            assertThat(result.get(2).getChangeType()).isEqualTo(PriceHistory.ChangeType.INITIAL);
        }

        @Test
        @DisplayName("No devuelve historial de otro producto")
        void shouldNotReturnHistoryFromOtherProduct() {
            createPriceHistory(product1, new BigDecimal("100.00"), null,
                    PriceHistory.ChangeType.INITIAL, null);
            createPriceHistory(product2, new BigDecimal("200.00"), null,
                    PriceHistory.ChangeType.INITIAL, null);

            List<PriceHistory> result = priceHistoryRepository.findByProductIdOrderByRecordedAtDesc(product1.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPrice()).isEqualByComparingTo(new BigDecimal("100.00"));
        }
    }

    @Nested
    @DisplayName("findByProductId (paginado)")
    class FindByProductPaginatedTests {

        @Test
        @DisplayName("Pagina correctamente el historial")
        void shouldPaginateHistory() {
            for (int i = 0; i < 15; i++) {
                createPriceHistory(product1,
                        new BigDecimal(100 + i),
                        i > 0 ? new BigDecimal(99 + i) : null,
                        i == 0 ? PriceHistory.ChangeType.INITIAL : PriceHistory.ChangeType.INCREASE,
                        null);
            }

            Page<PriceHistory> page0 = priceHistoryRepository.findByProductId(product1.getId(), PageRequest.of(0, 5));
            Page<PriceHistory> page1 = priceHistoryRepository.findByProductId(product1.getId(), PageRequest.of(1, 5));

            assertThat(page0.getContent()).hasSize(5);
            assertThat(page1.getContent()).hasSize(5);
            assertThat(page0.getTotalElements()).isEqualTo(15);
            assertThat(page0.getTotalPages()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("findByProductIdAndDateRange")
    class FindByDateRangeTests {

        @Test
        @DisplayName("Devuelve historial dentro del rango de fechas")
        void shouldReturnHistoryInDateRange() {
            LocalDateTime now = LocalDateTime.now();
            createPriceHistory(product1, new BigDecimal("100.00"), null,
                    PriceHistory.ChangeType.INITIAL, now.minusDays(10));
            createPriceHistory(product1, new BigDecimal("110.00"), new BigDecimal("100.00"),
                    PriceHistory.ChangeType.INCREASE, now.minusDays(5));
            createPriceHistory(product1, new BigDecimal("120.00"), new BigDecimal("110.00"),
                    PriceHistory.ChangeType.INCREASE, now.minusDays(1));

            List<PriceHistory> result = priceHistoryRepository.findByProductIdAndDateRange(
                    product1.getId(), now.minusDays(7), now);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Devuelve vacío si no hay registros en el rango")
        void shouldReturnEmptyWhenNoRecordsInRange() {
            LocalDateTime now = LocalDateTime.now();
            createPriceHistory(product1, new BigDecimal("100.00"), null,
                    PriceHistory.ChangeType.INITIAL, now.minusDays(30));

            List<PriceHistory> result = priceHistoryRepository.findByProductIdAndDateRange(
                    product1.getId(), now.minusDays(5), now);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findFirstByProductIdOrderByRecordedAtDesc")
    class FindLatestTests {

        @Test
        @DisplayName("Devuelve el registro más reciente")
        void shouldReturnLatestRecord() {
            createPriceHistory(product1, new BigDecimal("100.00"), null,
                    PriceHistory.ChangeType.INITIAL, LocalDateTime.now().minusDays(5));
            createPriceHistory(product1, new BigDecimal("150.00"), new BigDecimal("100.00"),
                    PriceHistory.ChangeType.INCREASE, LocalDateTime.now().minusDays(1));

            PriceHistory latest = priceHistoryRepository.findFirstByProductIdOrderByRecordedAtDesc(product1.getId());

            assertThat(latest).isNotNull();
            assertThat(latest.getPrice()).isEqualByComparingTo(new BigDecimal("150.00"));
        }

        @Test
        @DisplayName("Devuelve null si no hay historial")
        void shouldReturnNullWhenNoHistory() {
            PriceHistory latest = priceHistoryRepository.findFirstByProductIdOrderByRecordedAtDesc(product1.getId());

            assertThat(latest).isNull();
        }
    }

    @Nested
    @DisplayName("countPriceChangesSince")
    class CountChangesSinceTests {

        @Test
        @DisplayName("Cuenta cambios desde una fecha")
        void shouldCountChangesSinceDate() {
            LocalDateTime now = LocalDateTime.now();
            createPriceHistory(product1, new BigDecimal("100.00"), null,
                    PriceHistory.ChangeType.INITIAL, now.minusDays(30));
            createPriceHistory(product1, new BigDecimal("110.00"), new BigDecimal("100.00"),
                    PriceHistory.ChangeType.INCREASE, now.minusDays(3));
            createPriceHistory(product1, new BigDecimal("105.00"), new BigDecimal("110.00"),
                    PriceHistory.ChangeType.DECREASE, now.minusDays(1));

            long count = priceHistoryRepository.countPriceChangesSince(product1.getId(), now.minusDays(7));

            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("deleteByRecordedAtBefore (TTL)")
    class DeleteTTLTests {

        @Test
        @DisplayName("Elimina registros anteriores a la fecha dada")
        void shouldDeleteOldRecords() {
            LocalDateTime now = LocalDateTime.now();
            createPriceHistory(product1, new BigDecimal("100.00"), null,
                    PriceHistory.ChangeType.INITIAL, now.minusDays(400));
            createPriceHistory(product1, new BigDecimal("110.00"), new BigDecimal("100.00"),
                    PriceHistory.ChangeType.INCREASE, now.minusDays(5));

            priceHistoryRepository.deleteByRecordedAtBefore(now.minusDays(365));
            entityManager.flush();

            List<PriceHistory> remaining = priceHistoryRepository.findByProductIdOrderByRecordedAtDesc(product1.getId());
            assertThat(remaining).hasSize(1);
            assertThat(remaining.get(0).getPrice()).isEqualByComparingTo(new BigDecimal("110.00"));
        }
    }

    @Nested
    @DisplayName("findTop10ByProductIdOrderByRecordedAtDesc")
    class FindTop10Tests {

        @Test
        @DisplayName("Devuelve máximo 10 registros")
        void shouldReturnMax10Records() {
            for (int i = 0; i < 15; i++) {
                createPriceHistory(product1,
                        new BigDecimal(100 + i),
                        i > 0 ? new BigDecimal(99 + i) : null,
                        i == 0 ? PriceHistory.ChangeType.INITIAL : PriceHistory.ChangeType.INCREASE,
                        null);
            }

            List<PriceHistory> result = priceHistoryRepository.findTop10ByProductIdOrderByRecordedAtDesc(product1.getId());

            assertThat(result).hasSize(10);
        }
    }
}
