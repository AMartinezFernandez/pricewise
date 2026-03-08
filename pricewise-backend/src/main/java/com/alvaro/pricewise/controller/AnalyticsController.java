package com.alvaro.pricewise.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
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
import com.alvaro.pricewise.security.UserPrincipal;
import com.alvaro.pricewise.service.AnalyticsService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'EMPLOYEE', 'ADMIN')")
@Validated
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardMetrics>> getDashboardMetrics(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getDashboardMetrics(userPrincipal.requireCompanyId())));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<PageResponse<RecommendationSummary>>> getRecommendations(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getRecommendations(userPrincipal.requireCompanyId(), page, size)));
    }

    @PostMapping("/recommendations/{id}/apply")
    public ResponseEntity<ApiResponse<String>> applyRecommendation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        analyticsService.applyRecommendation(userPrincipal.requireCompanyId(), id);
        return ResponseEntity.ok(ApiResponse.success("Recomendacion aplicada"));
    }

    @PostMapping("/recommendations/{id}/dismiss")
    public ResponseEntity<ApiResponse<String>> dismissRecommendation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        analyticsService.dismissRecommendation(userPrincipal.requireCompanyId(), id);
        return ResponseEntity.ok(ApiResponse.success("Recomendacion descartada"));
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<PageResponse<AlertSummary>>> getAlerts(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "false") boolean onlyUnread) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getAlerts(userPrincipal.requireCompanyId(), page, size, onlyUnread)));
    }

    @PostMapping("/alerts/{id}/read")
    public ResponseEntity<ApiResponse<String>> markAlertAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        analyticsService.markAlertAsRead(userPrincipal.requireCompanyId(), id);
        return ResponseEntity.ok(ApiResponse.success("Alerta marcada como leida"));
    }

    @PostMapping("/alerts/read-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAllAlertsAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        int updated = analyticsService.markAllAlertsAsRead(userPrincipal.requireCompanyId());
        Map<String, Object> result = new HashMap<>();
        result.put("alertsMarkedAsRead", updated);
        return ResponseEntity.ok(ApiResponse.success(result, "Todas las alertas marcadas como leidas"));
    }

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runAnalysis(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        int analyzed = analyticsService.analyzeAllProducts(userPrincipal.requireCompanyId());
        Map<String, Object> result = new HashMap<>();
        result.put("productsAnalyzed", analyzed);
        result.put("message", "Analisis completado");
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
