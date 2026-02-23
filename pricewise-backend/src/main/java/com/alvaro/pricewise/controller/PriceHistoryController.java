package com.alvaro.pricewise.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alvaro.pricewise.dto.common.ApiResponse;
import com.alvaro.pricewise.dto.common.PageResponse;
import com.alvaro.pricewise.dto.history.PriceHistoryDTOs.PriceHistoryResponse;
import com.alvaro.pricewise.entity.PriceHistory;
import com.alvaro.pricewise.exception.ResourceNotFoundException;
import com.alvaro.pricewise.repository.PriceHistoryRepository;
import com.alvaro.pricewise.repository.ProductRepository;
import com.alvaro.pricewise.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products/{productId}/history")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'EMPLOYEE', 'ADMIN')")
@SuppressWarnings("null")
public class PriceHistoryController {

    private final PriceHistoryRepository priceHistoryRepository;
    private final ProductRepository productRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PriceHistoryResponse>>> getHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        validateProductOwnership(userPrincipal.getCompanyId(), productId);

        // Si hay filtro por fechas, devolver lista completa (no paginada)
        if (startDate != null && endDate != null) {
            List<PriceHistory> history = priceHistoryRepository
                    .findByProductIdAndDateRange(productId, startDate, endDate);
            List<PriceHistoryResponse> content = history.stream()
                    .map(PriceHistoryResponse::fromEntity)
                    .collect(Collectors.toList());

            PageResponse<PriceHistoryResponse> response = PageResponse.<PriceHistoryResponse>builder()
                    .content(content)
                    .pageNumber(0)
                    .pageSize(content.size())
                    .totalElements(content.size())
                    .totalPages(1)
                    .first(true)
                    .last(true)
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();

            return ResponseEntity.ok(ApiResponse.success(response));
        }

        // Paginación estándar con límite de tamaño
        int clampedSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, clampedSize, Sort.by("recordedAt").descending());
        Page<PriceHistory> historyPage = priceHistoryRepository.findByProductId(productId, pageable);

        List<PriceHistoryResponse> content = historyPage.getContent().stream()
                .map(PriceHistoryResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(historyPage, content)));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<PriceHistoryResponse>>> getRecentHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long productId) {

        validateProductOwnership(userPrincipal.getCompanyId(), productId);

        List<PriceHistoryResponse> history = priceHistoryRepository
                .findTop10ByProductIdOrderByRecordedAtDesc(productId)
                .stream()
                .map(PriceHistoryResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(history));
    }

    private void validateProductOwnership(Long companyId, Long productId) {
        productRepository.findByCompanyIdAndIdWithCreatedBy(companyId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
    }
}
