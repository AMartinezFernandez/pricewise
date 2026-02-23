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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("AlertRepository Integration Tests")
class AlertRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AlertRepository alertRepository;

    private Company company1;
    private Company company2;
    private User user1;
    private User user2;
    private Product product1;
    private Product product2;

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

        user1 = entityManager.persistAndFlush(User.builder()
                .username("user1")
                .email("user1@test.com")
                .password("encoded")
                .company(company1)
                .role(User.Role.COMPANY_ADMIN)
                .build());

        user2 = entityManager.persistAndFlush(User.builder()
                .username("user2")
                .email("user2@test.com")
                .password("encoded")
                .company(company2)
                .role(User.Role.COMPANY_ADMIN)
                .build());

        product1 = entityManager.persistAndFlush(Product.builder()
                .name("Producto 1")
                .sku("SKU-1")
                .currentPrice(new BigDecimal("100.00"))
                .company(company1)
                .createdBy(user1)
                .active(true)
                .monitoringEnabled(true)
                .build());

        product2 = entityManager.persistAndFlush(Product.builder()
                .name("Producto 2")
                .sku("SKU-2")
                .currentPrice(new BigDecimal("200.00"))
                .company(company2)
                .createdBy(user2)
                .active(true)
                .monitoringEnabled(true)
                .build());
    }

    private Alert createAlert(User user, Product product, Alert.AlertType type,
                              Alert.Severity severity, boolean isRead) {
        return entityManager.persistAndFlush(Alert.builder()
                .user(user)
                .product(product)
                .alertType(type)
                .title("Alerta: " + product.getName())
                .message("Cambio de precio detectado")
                .previousPrice(new BigDecimal("90.00"))
                .newPrice(new BigDecimal("100.00"))
                .changePercent(new BigDecimal("11.11"))
                .severity(severity)
                .isRead(isRead)
                .build());
    }

    @Nested
    @DisplayName("findByCompanyId")
    class FindByCompanyIdTests {

        @Test
        @DisplayName("Devuelve solo alertas de la empresa correcta")
        void shouldReturnOnlyAlertsFromCorrectCompany() {
            createAlert(user1, product1, Alert.AlertType.COMPETITOR_PRICE_DROP, Alert.Severity.INFO, false);
            createAlert(user1, product1, Alert.AlertType.COMPETITOR_PRICE_RISE, Alert.Severity.WARNING, false);
            createAlert(user2, product2, Alert.AlertType.PRICE_BELOW_COST, Alert.Severity.CRITICAL, false);

            Page<Alert> result = alertRepository.findByCompanyId(company1.getId(), PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).allSatisfy(alert ->
                    assertThat(alert.getProduct().getCompany().getId()).isEqualTo(company1.getId()));
        }

        @Test
        @DisplayName("Devuelve vacío si la empresa no tiene alertas")
        void shouldReturnEmptyWhenNoAlerts() {
            createAlert(user1, product1, Alert.AlertType.COMPETITOR_PRICE_DROP, Alert.Severity.INFO, false);

            Page<Alert> result = alertRepository.findByCompanyId(company2.getId(), PageRequest.of(0, 10));

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByCompanyIdAndIsReadFalse")
    class FindUnreadByCompanyTests {

        @Test
        @DisplayName("Devuelve solo alertas no leídas de la empresa")
        void shouldReturnOnlyUnreadAlerts() {
            createAlert(user1, product1, Alert.AlertType.COMPETITOR_PRICE_DROP, Alert.Severity.INFO, false);
            createAlert(user1, product1, Alert.AlertType.COMPETITOR_PRICE_RISE, Alert.Severity.WARNING, true);
            createAlert(user1, product1, Alert.AlertType.PRICE_BELOW_COST, Alert.Severity.CRITICAL, false);

            Page<Alert> result = alertRepository.findByCompanyIdAndIsReadFalse(company1.getId(), PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).allSatisfy(alert ->
                    assertThat(alert.getIsRead()).isFalse());
        }
    }

    @Nested
    @DisplayName("countByCompanyIdAndIsReadFalse")
    class CountUnreadTests {

        @Test
        @DisplayName("Cuenta correctamente alertas no leídas")
        void shouldCountUnreadAlerts() {
            createAlert(user1, product1, Alert.AlertType.COMPETITOR_PRICE_DROP, Alert.Severity.INFO, false);
            createAlert(user1, product1, Alert.AlertType.COMPETITOR_PRICE_RISE, Alert.Severity.INFO, true);
            createAlert(user1, product1, Alert.AlertType.PRICE_BELOW_COST, Alert.Severity.CRITICAL, false);

            long count = alertRepository.countByCompanyIdAndIsReadFalse(company1.getId());

            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("No cuenta alertas de otra empresa")
        void shouldNotCountAlertsFromOtherCompany() {
            createAlert(user1, product1, Alert.AlertType.COMPETITOR_PRICE_DROP, Alert.Severity.INFO, false);
            createAlert(user2, product2, Alert.AlertType.COMPETITOR_PRICE_DROP, Alert.Severity.INFO, false);

            long count = alertRepository.countByCompanyIdAndIsReadFalse(company1.getId());

            assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("markAllAsReadByCompanyId")
    class MarkAllAsReadTests {

        @Test
        @DisplayName("Marca todas las alertas de la empresa como leídas")
        void shouldMarkAllAlertsAsRead() {
            createAlert(user1, product1, Alert.AlertType.COMPETITOR_PRICE_DROP, Alert.Severity.INFO, false);
            createAlert(user1, product1, Alert.AlertType.COMPETITOR_PRICE_RISE, Alert.Severity.WARNING, false);
            createAlert(user1, product1, Alert.AlertType.PRICE_BELOW_COST, Alert.Severity.CRITICAL, false);

            int updated = alertRepository.markAllAsReadByCompanyId(company1.getId());
            entityManager.clear();

            assertThat(updated).isEqualTo(3);
            long unreadCount = alertRepository.countByCompanyIdAndIsReadFalse(company1.getId());
            assertThat(unreadCount).isZero();
        }

        @Test
        @DisplayName("No afecta alertas de otra empresa")
        void shouldNotAffectOtherCompanyAlerts() {
            createAlert(user1, product1, Alert.AlertType.COMPETITOR_PRICE_DROP, Alert.Severity.INFO, false);
            createAlert(user2, product2, Alert.AlertType.COMPETITOR_PRICE_DROP, Alert.Severity.INFO, false);

            alertRepository.markAllAsReadByCompanyId(company1.getId());
            entityManager.clear();

            long company2Unread = alertRepository.countByCompanyIdAndIsReadFalse(company2.getId());
            assertThat(company2Unread).isEqualTo(1);
        }

        @Test
        @DisplayName("No afecta alertas ya leídas (devuelve 0 si todas leídas)")
        void shouldReturnZeroWhenAllAlreadyRead() {
            createAlert(user1, product1, Alert.AlertType.COMPETITOR_PRICE_DROP, Alert.Severity.INFO, true);
            createAlert(user1, product1, Alert.AlertType.COMPETITOR_PRICE_RISE, Alert.Severity.WARNING, true);

            int updated = alertRepository.markAllAsReadByCompanyId(company1.getId());

            assertThat(updated).isZero();
        }
    }

}
