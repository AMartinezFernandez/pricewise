package com.alvaro.pricewise.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alvaro.pricewise.dto.analytics.AnalyticsDTOs.AlertSummary;
import com.alvaro.pricewise.dto.analytics.AnalyticsDTOs.DashboardMetrics;
import com.alvaro.pricewise.dto.analytics.AnalyticsDTOs.RecommendationSummary;
import com.alvaro.pricewise.dto.common.ApiResponse;
import com.alvaro.pricewise.dto.common.PageResponse;
import com.alvaro.pricewise.entity.Alert;
import com.alvaro.pricewise.entity.PriceRecommendation;
import com.alvaro.pricewise.repository.ProductRepository;
import com.alvaro.pricewise.security.UserPrincipal;
import com.alvaro.pricewise.service.PriceAnalysisService;

import lombok.RequiredArgsConstructor;

/**
 * Controlador de metricas, recomendaciones y alertas.
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'EMPLOYEE', 'ADMIN')")
@SuppressWarnings("null")
public class AnalyticsController {

    private final PriceAnalysisService priceAnalysisService;
    private final ProductRepository productRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardMetrics>> getDashboardMetrics(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Long companyId = userPrincipal.getCompanyId();

        long totalProducts = productRepository.countByCompanyIdAndActiveTrue(companyId);
        long activeMonitoring = productRepository.findByCompanyIdAndActiveTrue(companyId).stream()
                .filter(p -> p.getMonitoringEnabled() != null && p.getMonitoringEnabled())
                .count();

        long pendingRecs = priceAnalysisService.countPendingRecommendations(companyId);
        long unreadAlerts = priceAnalysisService.countUnreadAlerts(companyId);
        BigDecimal savings = priceAnalysisService.getTotalPotentialSavings(companyId);

        Page<PriceRecommendation> topRecs = priceAnalysisService
                .getPendingRecommendations(companyId, PageRequest.of(0, 5));

        List<RecommendationSummary> topRecommendations = topRecs.getContent().stream()
                .map(RecommendationSummary::fromEntity)
                .collect(Collectors.toList());

        Page<Alert> alerts = priceAnalysisService.getUnreadAlertsByCompany(companyId, PageRequest.of(0, 100));
        Map<String, Long> alertsByType = new HashMap<>();
        alerts.getContent().forEach(a ->
                alertsByType.merge(a.getAlertType().name(), 1L, (v1, v2) -> v1 + v2));

        DashboardMetrics metrics = DashboardMetrics.builder()
                .totalProducts(totalProducts)
                .activeMonitoring(activeMonitoring)
                .pendingRecommendations(pendingRecs)
                .unreadAlerts(unreadAlerts)
                .potentialSavings(savings)
                .topRecommendations(topRecommendations)
                .alertsByType(alertsByType)
                .build();

        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<PageResponse<RecommendationSummary>>> getRecommendations(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("priority").descending());
        Page<PriceRecommendation> recommendations = priceAnalysisService
                .getPendingRecommendations(userPrincipal.getCompanyId(), pageable);

        List<RecommendationSummary> content = recommendations.getContent().stream()
                .map(RecommendationSummary::fromEntity)
                .collect(Collectors.toList());

        PageResponse<RecommendationSummary> response = PageResponse.<RecommendationSummary>builder()
                .content(content)
                .pageNumber(page)
                .pageSize(size)
                .totalElements(recommendations.getTotalElements())
                .totalPages(recommendations.getTotalPages())
                .first(recommendations.isFirst())
                .last(recommendations.isLast())
                .hasNext(recommendations.hasNext())
                .hasPrevious(recommendations.hasPrevious())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/recommendations/{id}/apply")
    public ResponseEntity<ApiResponse<String>> applyRecommendation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {

        priceAnalysisService.applyRecommendation(userPrincipal.getCompanyId(), id);
        return ResponseEntity.ok(ApiResponse.success("Recomendacion aplicada"));
    }

    @PostMapping("/recommendations/{id}/dismiss")
    public ResponseEntity<ApiResponse<String>> dismissRecommendation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {

        priceAnalysisService.dismissRecommendation(userPrincipal.getCompanyId(), id);
        return ResponseEntity.ok(ApiResponse.success("Recomendacion descartada"));
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<PageResponse<AlertSummary>>> getAlerts(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean onlyUnread) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Alert> alerts = priceAnalysisService.getAlertsByCompany(userPrincipal.getCompanyId(), onlyUnread, pageable);

        List<AlertSummary> content = alerts.getContent().stream()
                .map(AlertSummary::fromEntity)
                .collect(Collectors.toList());

        PageResponse<AlertSummary> response = PageResponse.<AlertSummary>builder()
                .content(content)
                .pageNumber(page)
                .pageSize(size)
                .totalElements(alerts.getTotalElements())
                .totalPages(alerts.getTotalPages())
                .first(alerts.isFirst())
                .last(alerts.isLast())
                .hasNext(alerts.hasNext())
                .hasPrevious(alerts.hasPrevious())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/alerts/{id}/read")
    public ResponseEntity<ApiResponse<String>> markAlertAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {

        priceAnalysisService.markAlertAsRead(userPrincipal.getCompanyId(), id);
        return ResponseEntity.ok(ApiResponse.success("Alerta marcada como leida"));
    }

    @PostMapping("/alerts/read-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAllAlertsAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        int updated = priceAnalysisService.markAllAlertsAsRead(userPrincipal.getCompanyId());
        Map<String, Object> result = new HashMap<>();
        result.put("alertsMarkedAsRead", updated);
        return ResponseEntity.ok(ApiResponse.success(result, "Todas las alertas marcadas como leidas"));
    }

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runAnalysis(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        int analyzed = priceAnalysisService.analyzeAllProductsForUser(userPrincipal.getCompanyId());
        
        Map<String, Object> result = new HashMap<>();
        result.put("productsAnalyzed", analyzed);
        result.put("message", "Analisis completado");

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
