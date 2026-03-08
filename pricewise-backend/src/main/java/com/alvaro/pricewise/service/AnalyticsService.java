package com.alvaro.pricewise.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.pricewise.dto.analytics.AnalyticsDTOs.AlertSummary;
import com.alvaro.pricewise.dto.analytics.AnalyticsDTOs.DashboardMetrics;
import com.alvaro.pricewise.dto.analytics.AnalyticsDTOs.RecommendationSummary;
import com.alvaro.pricewise.dto.common.PageResponse;
import com.alvaro.pricewise.entity.Alert;
import com.alvaro.pricewise.entity.PriceRecommendation;
import com.alvaro.pricewise.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final PriceAnalysisService priceAnalysisService;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public DashboardMetrics getDashboardMetrics(Long companyId) {
        long totalProducts = productRepository.countByCompanyIdAndActiveTrue(companyId);
        long activeMonitoring = productRepository.countByCompanyIdAndMonitoringEnabledTrueAndActiveTrue(companyId);

        long pendingRecs = priceAnalysisService.countPendingRecommendations(companyId);
        long unreadAlerts = priceAnalysisService.countUnreadAlerts(companyId);
        BigDecimal savings = priceAnalysisService.getTotalPotentialSavings(companyId);

        List<RecommendationSummary> topRecommendations = priceAnalysisService
                .getPendingRecommendations(companyId, PageRequest.of(0, 5))
                .getContent().stream()
                .map(RecommendationSummary::fromEntity)
                .collect(Collectors.toList());

        Map<String, Long> alertsByType = new HashMap<>();
        priceAnalysisService.getAlertsByCompany(companyId, true, PageRequest.of(0, 100))
                .getContent()
                .forEach(a -> alertsByType.merge(a.getAlertType().name(), 1L, Long::sum));

        return DashboardMetrics.builder()
                .totalProducts(totalProducts)
                .activeMonitoring(activeMonitoring)
                .pendingRecommendations(pendingRecs)
                .unreadAlerts(unreadAlerts)
                .potentialSavings(savings)
                .topRecommendations(topRecommendations)
                .alertsByType(alertsByType)
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<RecommendationSummary> getRecommendations(Long companyId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("priority").descending());
        Page<PriceRecommendation> recommendations = priceAnalysisService
                .getPendingRecommendations(companyId, pageable);

        List<RecommendationSummary> content = recommendations.getContent().stream()
                .map(RecommendationSummary::fromEntity)
                .collect(Collectors.toList());

        return PageResponse.<RecommendationSummary>builder()
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
    }

    @Transactional(readOnly = true)
    public PageResponse<AlertSummary> getAlerts(Long companyId, int page, int size, boolean onlyUnread) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Alert> alerts = priceAnalysisService.getAlertsByCompany(companyId, onlyUnread, pageable);

        List<AlertSummary> content = alerts.getContent().stream()
                .map(AlertSummary::fromEntity)
                .collect(Collectors.toList());

        return PageResponse.<AlertSummary>builder()
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
    }

    public void applyRecommendation(Long companyId, Long id) {
        priceAnalysisService.applyRecommendation(companyId, id);
    }

    public void dismissRecommendation(Long companyId, Long id) {
        priceAnalysisService.dismissRecommendation(companyId, id);
    }

    public void markAlertAsRead(Long companyId, Long id) {
        priceAnalysisService.markAlertAsRead(companyId, id);
    }

    public int markAllAlertsAsRead(Long companyId) {
        return priceAnalysisService.markAllAlertsAsRead(companyId);
    }

    public int analyzeAllProducts(Long companyId) {
        return priceAnalysisService.analyzeAllProductsForUser(companyId);
    }
}
