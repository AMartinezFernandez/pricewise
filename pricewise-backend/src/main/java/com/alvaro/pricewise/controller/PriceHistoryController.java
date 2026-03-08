package com.alvaro.pricewise.controller;

import java.time.LocalDateTime;
import java.util.List;

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
import com.alvaro.pricewise.security.UserPrincipal;
import com.alvaro.pricewise.service.PriceHistoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products/{productId}/history")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'EMPLOYEE', 'ADMIN')")
public class PriceHistoryController {

    private final PriceHistoryService priceHistoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PriceHistoryResponse>>> getHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        PageResponse<PriceHistoryResponse> response = priceHistoryService.getHistory(
                userPrincipal.getCompanyId(), productId, page, size, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<PriceHistoryResponse>>> getRecentHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long productId) {

        List<PriceHistoryResponse> history = priceHistoryService.getRecentHistory(
                userPrincipal.getCompanyId(), productId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
