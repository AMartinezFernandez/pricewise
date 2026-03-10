package com.alvaro.pricewise.dto.product;

import com.alvaro.pricewise.entity.CompetitorPrice;
import com.alvaro.pricewise.entity.Product;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductDTOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateProductRequest {
        @NotBlank(message = "El nombre del producto es obligatorio")
        @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
        private String name;

        @Size(max = 1000, message = "La descripción no puede exceder 1000 caracteres")
        private String description;

        @Size(max = 50, message = "El SKU no puede exceder 50 caracteres")
        private String sku;

        @Size(max = 20, message = "El ASIN no puede exceder 20 caracteres")
        private String asin;

        @Size(max = 50, message = "El EAN no puede exceder 50 caracteres")
        private String ean;

        @NotNull(message = "El precio de venta es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
        private BigDecimal currentPrice;

        @NotNull(message = "El precio de coste es obligatorio")
        @DecimalMin(value = "0.00", message = "El coste no puede ser negativo")
        private BigDecimal costPrice;

        @DecimalMin(value = "0.00", message = "El margen mínimo no puede ser negativo")
        private BigDecimal minMargin;

        @Size(max = 100)
        private String category;

        @Size(max = 100)
        private String brand;

        @Size(max = 500)
        private String imageUrl;

        private Boolean monitoringEnabled;

        @Min(value = 0, message = "El stock no puede ser negativo")
        private Integer stockQuantity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateProductRequest {
        @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
        private String name;

        @Size(max = 1000, message = "La descripción no puede exceder 1000 caracteres")
        private String description;

        @Size(max = 50, message = "El SKU no puede exceder 50 caracteres")
        private String sku;

        @Size(max = 20, message = "El ASIN no puede exceder 20 caracteres")
        private String asin;

        @Size(max = 50, message = "El EAN no puede exceder 50 caracteres")
        private String ean;

        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
        private BigDecimal currentPrice;

        @DecimalMin(value = "0.00", message = "El coste no puede ser negativo")
        private BigDecimal costPrice;

        @DecimalMin(value = "0.00", message = "El margen mínimo no puede ser negativo")
        private BigDecimal minMargin;

        @Size(max = 100)
        private String category;

        @Size(max = 100)
        private String brand;

        @Size(max = 500)
        private String imageUrl;

        private Boolean monitoringEnabled;
        
        private Boolean active;

        @Min(value = 0, message = "El stock no puede ser negativo")
        private Integer stockQuantity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductResponse {
        private Long id;
        private String name;
        private String description;
        private String sku;
        private String asin;
        private String ean;
        private BigDecimal currentPrice;
        private BigDecimal costPrice;
        private BigDecimal minMargin;
        private BigDecimal margin;

        private String category;
        private String brand;
        private String imageUrl;

        private Boolean active;
        private Boolean monitoringEnabled;
        private Integer stockQuantity;
        private String createdByUsername;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        // Último precio de Amazon (persistido en competitor_prices)
        private BigDecimal amazonPrice;
        private String amazonProductTitle;
        private LocalDateTime amazonPriceUpdatedAt;

        public static ProductResponse fromEntity(Product product) {
            return ProductResponse.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .description(product.getDescription())
                    .sku(product.getSku())
                    .asin(product.getAsin())
                    .ean(product.getEan())
                    .currentPrice(product.getCurrentPrice())
                    .costPrice(product.getCostPrice())
                    .minMargin(product.getMinMargin())
                    .margin(product.getMargin())

                    .category(product.getCategory())

                    .brand(product.getBrand())
                    .imageUrl(product.getImageUrl())
                    .active(product.getActive())
                    .monitoringEnabled(product.getMonitoringEnabled())
                    .stockQuantity(product.getStockQuantity())
                    .createdByUsername(product.getCreatedBy() != null ? product.getCreatedBy().getUsername() : null)
                    .createdAt(product.getCreatedAt())
                    .updatedAt(product.getUpdatedAt())
                    .build();
        }

        public static ProductResponse fromEntity(Product product, CompetitorPrice competitorPrice) {
            ProductResponseBuilder builder = ProductResponse.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .description(product.getDescription())
                    .sku(product.getSku())
                    .asin(product.getAsin())
                    .ean(product.getEan())
                    .currentPrice(product.getCurrentPrice())
                    .costPrice(product.getCostPrice())
                    .minMargin(product.getMinMargin())
                    .margin(product.getMargin())
                    .category(product.getCategory())
                    .brand(product.getBrand())
                    .imageUrl(product.getImageUrl())
                    .active(product.getActive())
                    .monitoringEnabled(product.getMonitoringEnabled())
                    .stockQuantity(product.getStockQuantity())
                    .createdByUsername(product.getCreatedBy() != null ? product.getCreatedBy().getUsername() : null)
                    .createdAt(product.getCreatedAt())
                    .updatedAt(product.getUpdatedAt());

            if (competitorPrice != null) {
                builder.amazonPrice(competitorPrice.getPrice())
                        .amazonProductTitle(competitorPrice.getCompetitorProductTitle())
                        .amazonPriceUpdatedAt(competitorPrice.getScrapedAt());
            }

            return builder.build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductListResponse {
        private Long id;
        private String name;
        private String sku;
        private String asin;
        private BigDecimal currentPrice;
        private BigDecimal margin;
        private String category;
        private String brand;
        private Boolean monitoringEnabled;
        private Integer stockQuantity;

        public static ProductListResponse fromEntity(Product product) {
            return ProductListResponse.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .sku(product.getSku())
                    .asin(product.getAsin())
                    .currentPrice(product.getCurrentPrice())
                    .margin(product.getMargin())
                    .category(product.getCategory())
                    .brand(product.getBrand())
                    .monitoringEnabled(product.getMonitoringEnabled())
                    .stockQuantity(product.getStockQuantity())
                    .build();
        }
    }
}
