package com.alvaro.pricewise.dto.history;

import com.alvaro.pricewise.entity.PriceHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PriceHistoryDTOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PriceHistoryResponse {
        private Long id;
        private BigDecimal price;
        private BigDecimal previousPrice;
        private String changeType;
        private String changeReason;
        private BigDecimal percentageChange;
        private LocalDateTime recordedAt;

        public static PriceHistoryResponse fromEntity(PriceHistory ph) {
            return PriceHistoryResponse.builder()
                    .id(ph.getId())
                    .price(ph.getPrice())
                    .previousPrice(ph.getPreviousPrice())
                    .changeType(ph.getChangeType() != null ? ph.getChangeType().name() : null)
                    .changeReason(ph.getChangeReason())
                    .percentageChange(ph.getPercentageChange())
                    .recordedAt(ph.getRecordedAt())
                    .build();
        }
    }
}
