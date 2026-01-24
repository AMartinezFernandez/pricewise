package com.alvaro.pricewise.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_history",
       indexes = {
           @Index(name = "idx_price_history_product", columnList = "product_id"),
           @Index(name = "idx_price_history_date", columnList = "recorded_at")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Product product;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(precision = 12, scale = 2)
    private BigDecimal previousPrice;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ChangeType changeType;

    @Column(length = 200)
    private String changeReason;  // "Manual", "Recomendación aplicada", etc.

    @CreationTimestamp
    @Column(name = "recorded_at", updatable = false)
    private LocalDateTime recordedAt;

    public enum ChangeType {
        INCREASE,
        DECREASE,
        NO_CHANGE,
        INITIAL
    }

    // Método helper para calcular variación porcentual
    public BigDecimal getPercentageChange() {
        if (previousPrice == null || previousPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return price.subtract(previousPrice)
                .divide(previousPrice, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}
