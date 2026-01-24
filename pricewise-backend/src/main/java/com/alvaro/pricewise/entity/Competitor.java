package com.alvaro.pricewise.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entidad que representa un competidor (tienda online) del cual se monitorizan precios.
 * Ejemplos: Amazon, El Corte Inglés, MediaMarkt
 */
@Entity
@Table(name = "competitors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Competitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;  // Ej: "Amazon España", "El Corte Inglés", "MediaMarkt"

    @Column(nullable = false)
    private String code;  // Ej: "AMAZON_ES", "ECI", "MEDIAMARKT" - para identificación en código

    @Column(nullable = false)
    private String baseUrl;  // Ej: "https://www.amazon.es"

    private String logoUrl;  // URL del logo del competidor

    /**
     * Tipo de fuente de datos:
     * - API: Usa una API externa (Keepa, Canopy, etc.)
     * - SCRAPING: Usa Jsoup para scraping directo
     * - MANUAL: Precios introducidos manualmente
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceType sourceType;

    /**
     * Para APIs: clave de configuración o endpoint
     * Para Scraping: selector CSS del precio
     */
    private String sourceConfig;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    private LocalDateTime lastScrapedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Enum para definir el tipo de fuente de datos
     */
    public enum SourceType {
        API,        // Datos obtenidos de una API externa
        SCRAPING,   // Datos obtenidos mediante web scraping
        MANUAL      // Datos introducidos manualmente
    }
}
