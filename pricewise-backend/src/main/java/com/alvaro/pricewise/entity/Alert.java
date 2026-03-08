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
 * Alerta generada por cambios significativos en precios.
 */
@Entity
@Table(name = "alerts",
       indexes = {
           @Index(name = "idx_alert_user", columnList = "user_id"),
           @Index(name = "idx_alert_product", columnList = "product_id"),
           @Index(name = "idx_alert_read", columnList = "is_read"),
           @Index(name = "idx_alert_type", columnList = "alert_type")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 30)
    private AlertType alertType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String message;

    @Column(precision = 12, scale = 2)
    private BigDecimal previousPrice;

    @Column(precision = 12, scale = 2)
    private BigDecimal newPrice;

    @Column(precision = 5, scale = 2)
    private BigDecimal changePercent;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Severity severity = Severity.INFO;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime readAt;

    public enum AlertType {
        COMPETITOR_PRICE_DROP,    // Competidor bajo precio
        COMPETITOR_PRICE_RISE,    // Competidor subio precio
        COMPETITOR_OUT_OF_STOCK,  // Competidor sin stock
        PRICE_BELOW_COST,         // Precio por debajo del coste
        HIGH_MARGIN_OPPORTUNITY,  // Oportunidad de aumentar margen
        PRICE_MATCH_NEEDED        // Necesario igualar precio
    }

    public enum Severity {
        INFO,
        WARNING,
        CRITICAL
    }
}
