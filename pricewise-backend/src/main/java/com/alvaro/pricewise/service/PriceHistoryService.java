package com.alvaro.pricewise.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.pricewise.dto.common.PageResponse;
import com.alvaro.pricewise.dto.history.PriceHistoryDTOs.PriceHistoryResponse;
import com.alvaro.pricewise.entity.PriceHistory;
import com.alvaro.pricewise.exception.ResourceNotFoundException;
import com.alvaro.pricewise.repository.PriceHistoryRepository;
import com.alvaro.pricewise.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PriceHistoryService {

    private final PriceHistoryRepository priceHistoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public PageResponse<PriceHistoryResponse> getHistory(Long companyId, Long productId,
            int page, int size, LocalDateTime startDate, LocalDateTime endDate) {

        validateProductOwnership(companyId, productId);

        if (startDate != null && endDate != null) {
            List<PriceHistoryResponse> content = priceHistoryRepository
                    .findByProductIdAndDateRange(productId, startDate, endDate)
                    .stream()
                    .map(PriceHistoryResponse::fromEntity)
                    .toList();

            return PageResponse.<PriceHistoryResponse>builder()
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
        }

        int clampedSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, clampedSize, Sort.by("recordedAt").descending());
        Page<PriceHistory> historyPage = priceHistoryRepository.findByProductId(productId, pageable);

        List<PriceHistoryResponse> content = historyPage.getContent().stream()
                .map(PriceHistoryResponse::fromEntity)
                .toList();

        return PageResponse.from(historyPage, content);
    }

    @Transactional(readOnly = true)
    public List<PriceHistoryResponse> getRecentHistory(Long companyId, Long productId) {
        validateProductOwnership(companyId, productId);

        return priceHistoryRepository
                .findTop10ByProductIdOrderByRecordedAtDesc(productId)
                .stream()
                .map(PriceHistoryResponse::fromEntity)
                .toList();
    }

    private void validateProductOwnership(Long companyId, Long productId) {
        productRepository.findByCompanyIdAndIdWithCreatedBy(companyId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
    }
}
