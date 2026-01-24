package com.alvaro.pricewise.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Recomendacion de precio generada por el sistema.
 */
@Entity
@Table(name = "price_recommendations",
       indexes = {
           @Index(name = "idx_recommendation_product", columnList = "product_id"),
           @Index(name = "idx_recommendation_status", columnList = "status"),
           @Index(name = "idx_recommendation_type", columnList = "recommendation_type")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecommendationType recommendationType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal currentPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal competitorPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal suggestedPrice;

    @Column(precision = 5, scale = 2)
    private BigDecimal priceDifferencePercent;

    @Column(precision = 12, scale = 2)
    private BigDecimal potentialSavingOrProfit;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime appliedAt;

    private LocalDateTime dismissedAt;

    public enum RecommendationType {
        PRICE_TOO_HIGH,      // Precio muy alto vs competencia
        PRICE_TOO_LOW,       // Precio muy bajo, oportunidad subir
        COMPETITOR_DROP,     // Competidor bajo precio bruscamente
        COMPETITOR_RISE,     // Competidor subio precio
        OUT_OF_STOCK,        // Competidor sin stock
        NEW_COMPETITOR       // Nuevo competidor detectado
    }

    public enum Status {
        PENDING,             // Pendiente de accion
        APPLIED,             // Recomendacion aplicada
        DISMISSED,           // Descartada por usuario
        EXPIRED              // Expirada sin accion
    }

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }
}
