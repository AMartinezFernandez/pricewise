package com.alvaro.pricewise.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    // Métricas generales
    private long totalUsers;
    private long activeUsers;
    private long totalProducts;
    private long trackedProducts;
    
    // Métricas de precios
    private long productsWithPriceDrop; // Últimas 24h
    private long competitorsTracked;
    
    // Métricas del sistema
    private boolean keepaStatus;
    private String schedulerStatus;
    
    // Distribución (para gráficos)
    private Map<String, Long> productsByCategory;
}
