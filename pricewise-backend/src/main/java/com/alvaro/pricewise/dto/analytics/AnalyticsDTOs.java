package com.alvaro.pricewise.dto.analytics;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.alvaro.pricewise.entity.Alert;
import com.alvaro.pricewise.entity.PriceRecommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AnalyticsDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardMetrics {
        private long totalProducts;
        private long activeMonitoring;
        private long pendingRecommendations;
        private long unreadAlerts;
        private BigDecimal potentialSavings;
        private List<RecommendationSummary> topRecommendations;
        private Map<String, Long> alertsByType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendationSummary {
        private Long id;
        private Long productId;
        private String productName;
        private String recommendationType;
        private BigDecimal currentPrice;
        private BigDecimal competitorPrice;
        private BigDecimal suggestedPrice;
        private BigDecimal priceDifferencePercent;
        private String priority;
        private String reason;
        private LocalDateTime createdAt;

        public static RecommendationSummary fromEntity(PriceRecommendation r) {
            return RecommendationSummary.builder()
                    .id(r.getId())
                    .productId(r.getProduct().getId())
                    .productName(r.getProduct().getName())
                    .recommendationType(r.getRecommendationType().name())
                    .currentPrice(r.getCurrentPrice())
                    .competitorPrice(r.getCompetitorPrice())
                    .suggestedPrice(r.getSuggestedPrice())
                    .priceDifferencePercent(r.getPriceDifferencePercent())
                    .priority(r.getPriority().name())
                    .reason(r.getReason())
                    .createdAt(r.getCreatedAt())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertSummary {
        private Long id;
        private Long productId;
        private String productName;
        private String alertType;
        private String title;
        private String message;
        private BigDecimal previousPrice;
        private BigDecimal newPrice;
        private BigDecimal changePercent;
        private String severity;
        private Boolean isRead;
        private LocalDateTime createdAt;

        public static AlertSummary fromEntity(Alert a) {
            return AlertSummary.builder()
                    .id(a.getId())
                    .productId(a.getProduct().getId())
                    .productName(a.getProduct().getName())
                    .alertType(a.getAlertType().name())
                    .title(a.getTitle())
                    .message(a.getMessage())
                    .previousPrice(a.getPreviousPrice())
                    .newPrice(a.getNewPrice())
                    .changePercent(a.getChangePercent())
                    .severity(a.getSeverity().name())
                    .isRead(a.getIsRead())
                    .createdAt(a.getCreatedAt())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceHistoryPoint {
        private LocalDateTime date;
        private BigDecimal ourPrice;
        private BigDecimal competitorPrice;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductPriceComparison {
        private Long productId;
        private String productName;
        private BigDecimal ourPrice;
        private BigDecimal competitorPrice;
        private BigDecimal difference;
        private BigDecimal differencePercent;
        private String status; // HIGHER, LOWER, EQUAL
    }
}
