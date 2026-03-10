package com.alvaro.pricewise.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.alvaro.pricewise.dto.alertrule.AlertRuleDTOs.AlertRuleResponse;
import com.alvaro.pricewise.dto.alertrule.AlertRuleDTOs.CreateAlertRuleRequest;
import com.alvaro.pricewise.dto.alertrule.AlertRuleDTOs.UpdateAlertRuleRequest;
import com.alvaro.pricewise.entity.Alert;
import com.alvaro.pricewise.entity.AlertRule;
import com.alvaro.pricewise.entity.Company;
import com.alvaro.pricewise.entity.Product;
import com.alvaro.pricewise.exception.BadRequestException;
import com.alvaro.pricewise.exception.ResourceNotFoundException;
import com.alvaro.pricewise.repository.AlertRuleRepository;
import com.alvaro.pricewise.repository.CompanyRepository;
import com.alvaro.pricewise.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertRuleService Tests")
class AlertRuleServiceTest {

    @Mock private AlertRuleRepository alertRuleRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private ProductRepository productRepository;

    private AlertRuleService alertRuleService;
    private Company testCompany;

    @BeforeEach
    void setUp() {
        alertRuleService = new AlertRuleService(alertRuleRepository, companyRepository, productRepository);

        testCompany = Company.builder()
                .id(1L)
                .name("Test Company")
                .companyCode("ABCD1234")
                .build();
    }

    private AlertRule buildRule(Long id, Alert.AlertType type, Product product) {
        return AlertRule.builder()
                .id(id)
                .company(testCompany)
                .product(product)
                .alertType(type)
                .name("Test Rule")
                .threshold(new BigDecimal("10.00"))
                .enabled(true)
                .build();
    }

    @Nested
    @DisplayName("getRules")
    class GetRulesTests {

        @Test
        @DisplayName("Devuelve reglas de la empresa")
        void returnsRulesForCompany() {
            AlertRule rule = buildRule(1L, Alert.AlertType.COMPETITOR_PRICE_DROP, null);
            when(alertRuleRepository.findByCompanyIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(rule));

            List<AlertRuleResponse> result = alertRuleService.getRules(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAlertType()).isEqualTo("COMPETITOR_PRICE_DROP");
        }

        @Test
        @DisplayName("Devuelve lista vacia si no hay reglas")
        void returnsEmptyList() {
            when(alertRuleRepository.findByCompanyIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of());

            assertThat(alertRuleService.getRules(1L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("createRule")
    class CreateRuleTests {

        @Test
        @DisplayName("Crea regla sin producto")
        void createsRuleWithoutProduct() {
            when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));
            when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(inv -> {
                AlertRule r = inv.getArgument(0);
                r.setId(10L);
                return r;
            });

            CreateAlertRuleRequest request = new CreateAlertRuleRequest();
            request.setAlertType("COMPETITOR_PRICE_DROP");
            request.setThreshold(new BigDecimal("15.00"));
            request.setName("Mi regla");

            AlertRuleResponse response = alertRuleService.createRule(1L, request);

            assertThat(response.getAlertType()).isEqualTo("COMPETITOR_PRICE_DROP");
            assertThat(response.getName()).isEqualTo("Mi regla");
            assertThat(response.getProductId()).isNull();

            ArgumentCaptor<AlertRule> captor = ArgumentCaptor.forClass(AlertRule.class);
            verify(alertRuleRepository).save(captor.capture());
            assertThat(captor.getValue().getEnabled()).isTrue();
        }

        @Test
        @DisplayName("Crea regla con producto")
        void createsRuleWithProduct() {
            Product product = Product.builder().id(5L).name("Test Product").company(testCompany).build();
            when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));
            when(productRepository.findById(5L)).thenReturn(Optional.of(product));
            when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(inv -> {
                AlertRule r = inv.getArgument(0);
                r.setId(10L);
                return r;
            });

            CreateAlertRuleRequest request = new CreateAlertRuleRequest();
            request.setAlertType("PRICE_BELOW_COST");
            request.setThreshold(new BigDecimal("5.00"));
            request.setProductId(5L);

            AlertRuleResponse response = alertRuleService.createRule(1L, request);

            assertThat(response.getProductId()).isEqualTo(5L);
            assertThat(response.getProductName()).isEqualTo("Test Product");
        }

        @Test
        @DisplayName("Rechaza tipo de alerta invalido")
        void rejectsInvalidAlertType() {
            CreateAlertRuleRequest request = new CreateAlertRuleRequest();
            request.setAlertType("INVALID_TYPE");
            request.setThreshold(new BigDecimal("10.00"));

            assertThatThrownBy(() -> alertRuleService.createRule(1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Tipo de alerta no válido");
        }

        @Test
        @DisplayName("Rechaza producto de otra empresa")
        void rejectsProductFromOtherCompany() {
            Company otherCompany = Company.builder().id(99L).build();
            Product foreignProduct = Product.builder().id(5L).company(otherCompany).build();

            when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));
            when(productRepository.findById(5L)).thenReturn(Optional.of(foreignProduct));

            CreateAlertRuleRequest request = new CreateAlertRuleRequest();
            request.setAlertType("COMPETITOR_PRICE_DROP");
            request.setThreshold(new BigDecimal("10.00"));
            request.setProductId(5L);

            assertThatThrownBy(() -> alertRuleService.createRule(1L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Producto no encontrado");
        }
    }

    @Nested
    @DisplayName("updateRule")
    class UpdateRuleTests {

        @Test
        @DisplayName("Actualiza threshold y nombre")
        void updatesThresholdAndName() {
            AlertRule existing = buildRule(1L, Alert.AlertType.COMPETITOR_PRICE_DROP, null);
            when(alertRuleRepository.findByIdAndCompanyId(1L, 1L)).thenReturn(Optional.of(existing));
            when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateAlertRuleRequest request = new UpdateAlertRuleRequest();
            request.setThreshold(new BigDecimal("20.00"));
            request.setName("Updated Name");

            AlertRuleResponse response = alertRuleService.updateRule(1L, 1L, request);

            assertThat(response.getThreshold()).isEqualByComparingTo("20.00");
            assertThat(response.getName()).isEqualTo("Updated Name");
        }

        @Test
        @DisplayName("Lanza excepcion si regla no existe")
        void throwsIfRuleNotFound() {
            when(alertRuleRepository.findByIdAndCompanyId(999L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> alertRuleService.updateRule(1L, 999L, new UpdateAlertRuleRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteRule")
    class DeleteRuleTests {

        @Test
        @DisplayName("Elimina regla existente")
        void deletesExistingRule() {
            AlertRule rule = buildRule(1L, Alert.AlertType.COMPETITOR_PRICE_DROP, null);
            when(alertRuleRepository.findByIdAndCompanyId(1L, 1L)).thenReturn(Optional.of(rule));

            alertRuleService.deleteRule(1L, 1L);

            verify(alertRuleRepository).delete(rule);
        }

        @Test
        @DisplayName("Lanza excepcion si regla no existe")
        void throwsIfRuleNotFound() {
            when(alertRuleRepository.findByIdAndCompanyId(999L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> alertRuleService.deleteRule(1L, 999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("toggleRule")
    class ToggleRuleTests {

        @Test
        @DisplayName("Cambia enabled de true a false")
        void togglesToFalse() {
            AlertRule rule = buildRule(1L, Alert.AlertType.COMPETITOR_PRICE_DROP, null);
            rule.setEnabled(true);
            when(alertRuleRepository.findByIdAndCompanyId(1L, 1L)).thenReturn(Optional.of(rule));
            when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(inv -> inv.getArgument(0));

            AlertRuleResponse response = alertRuleService.toggleRule(1L, 1L);

            assertThat(response.getEnabled()).isFalse();
        }

        @Test
        @DisplayName("Cambia enabled de false a true")
        void togglesToTrue() {
            AlertRule rule = buildRule(1L, Alert.AlertType.COMPETITOR_PRICE_DROP, null);
            rule.setEnabled(false);
            when(alertRuleRepository.findByIdAndCompanyId(1L, 1L)).thenReturn(Optional.of(rule));
            when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(inv -> inv.getArgument(0));

            AlertRuleResponse response = alertRuleService.toggleRule(1L, 1L);

            assertThat(response.getEnabled()).isTrue();
        }
    }
}
