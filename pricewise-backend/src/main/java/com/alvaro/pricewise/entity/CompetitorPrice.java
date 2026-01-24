package com.alvaro.pricewise.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que almacena los precios de productos en tiendas de la competencia.
 * Cada registro representa un precio capturado en un momento específico.
 */
@Entity
@Table(name = "competitor_prices", indexes = {
    @Index(name = "idx_competitor_price_product", columnList = "product_id"),
    @Index(name = "idx_competitor_price_competitor", columnList = "competitor_id"),
    @Index(name = "idx_competitor_price_scraped_at", columnList = "scrapedAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetitorPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Producto propio al que se asocia este precio de competencia
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Competidor del que se obtuvo el precio
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competitor_id", nullable = false)
    private Competitor competitor;

    /**
     * URL directa al producto en la tienda del competidor
     */
    @Column(length = 1000)
    private String productUrl;

    /**
     * Título del producto en la tienda del competidor (puede diferir del nuestro)
     */
    private String competitorProductTitle;

    /**
     * Precio del producto en el competidor
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Precio original (sin descuento) si aplica
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal originalPrice;

    /**
     * Moneda del precio (EUR, USD, etc.)
     */
    @Column(length = 3)
    @Builder.Default
    private String currency = "EUR";

    /**
     * Si el producto está disponible para compra
     */
    @Builder.Default
    private boolean available = true;

    /**
     * Si el producto tiene envío gratuito
     */
    @Builder.Default
    private boolean freeShipping = false;

    /**
     * Coste de envío si no es gratuito
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal shippingCost;

    /**
     * Fecha y hora en que se obtuvo este precio
     */
    @Column(nullable = false)
    private LocalDateTime scrapedAt;

    /**
     * Fuente específica de datos (para debugging)
     * Ej: "keepa-api", "jsoup-scraper", "manual"
     */
    private String source;

    /**
     * Diferencia porcentual respecto a nuestro precio
     * Positivo = competidor más caro, Negativo = competidor más barato
     */
    @Transient
    public BigDecimal getPriceDifferencePercent() {
        if (product == null || product.getCurrentPrice() == null || 
            product.getCurrentPrice().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        // ((nuestro_precio - precio_competidor) / nuestro_precio) * 100
        return product.getCurrentPrice()
                .subtract(this.price)
                .divide(product.getCurrentPrice(), 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    @PrePersist
    protected void onCreate() {
        if (scrapedAt == null) {
            scrapedAt = LocalDateTime.now();
        }
    }
}
