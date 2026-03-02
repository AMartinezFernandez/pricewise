package com.alvaro.pricewise.dto.alertrule;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.alvaro.pricewise.entity.AlertRule;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AlertRuleDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertRuleResponse {
        private Long id;
        private String alertType;
        private String name;
        private BigDecimal threshold;
        private BigDecimal targetPrice;
        private Boolean enabled;
        private Long productId;
        private String productName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static AlertRuleResponse fromEntity(AlertRule r) {
            return AlertRuleResponse.builder()
                    .id(r.getId())
                    .alertType(r.getAlertType().name())
                    .name(r.getName())
                    .threshold(r.getThreshold())
                    .targetPrice(r.getTargetPrice())
                    .enabled(r.getEnabled())
                    .productId(r.getProduct() != null ? r.getProduct().getId() : null)
                    .productName(r.getProduct() != null ? r.getProduct().getName() : null)
                    .createdAt(r.getCreatedAt())
                    .updatedAt(r.getUpdatedAt())
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateAlertRuleRequest {
        @NotBlank(message = "El tipo de alerta es obligatorio")
        private String alertType;

        @NotNull(message = "El umbral es obligatorio")
        @DecimalMin(value = "0.01", message = "El umbral debe ser mayor que 0")
        private BigDecimal threshold;

        private String name;
        private Long productId;

        @DecimalMin(value = "0.01", message = "El precio objetivo debe ser mayor que 0")
        private BigDecimal targetPrice;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateAlertRuleRequest {
        @DecimalMin(value = "0.01", message = "El umbral debe ser mayor que 0")
        private BigDecimal threshold;

        private Boolean enabled;
        private String name;

        @DecimalMin(value = "0.01", message = "El precio objetivo debe ser mayor que 0")
        private BigDecimal targetPrice;
    }
}
