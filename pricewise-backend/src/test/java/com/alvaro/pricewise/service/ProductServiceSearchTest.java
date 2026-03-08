package com.alvaro.pricewise.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.alvaro.pricewise.dto.common.PageResponse;
import com.alvaro.pricewise.dto.product.ProductDTOs.ProductListResponse;
import com.alvaro.pricewise.entity.CompetitorPrice;
import com.alvaro.pricewise.entity.Product;
import com.alvaro.pricewise.repository.CompanyRepository;
import com.alvaro.pricewise.repository.CompetitorPriceRepository;
import com.alvaro.pricewise.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceSearchTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private KeepaService keepaService;
    @Mock
    private com.alvaro.pricewise.repository.UserRepository userRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private com.alvaro.pricewise.repository.PriceHistoryRepository priceHistoryRepository;
    @Mock
    private CompetitorPriceRepository competitorPriceRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, userRepository, companyRepository, priceHistoryRepository, competitorPriceRepository, keepaService);
    }

    @Test
    void searchProducts_NormalText_ReturnsLocalResults() {
        // Arrange: companyId instead of userId
        Long companyId = 1L;
        String searchText = "iphone";
        Pageable pageable = PageRequest.of(0, 10);
        
        Product product = Product.builder().id(1L).name("iPhone 13").build();
        Page<Product> page = new PageImpl<>(List.of(product));
        
        when(productRepository.searchProducts(eq(companyId), eq(searchText), any(), any(), eq(pageable)))
                .thenReturn(page);

        // Act
        PageResponse<ProductListResponse> response = productService.searchProducts(companyId, searchText, null, null, pageable);

        // Assert
        assertEquals(1, response.getTotalElements());
        assertEquals("iPhone 13", response.getContent().get(0).getName());
        verify(keepaService, never()).fetchPriceByAsin(any(), any(), any());
    }

    @Test
    void searchProducts_AsinInDb_ReturnsLocalResults() {
        // Arrange
        Long companyId = 1L;
        String asin = "B08H93ZRK9";
        Pageable pageable = PageRequest.of(0, 10);

        Product product = Product.builder().id(1L).name("Product with ASIN").sku(asin).build();
        Page<Product> page = new PageImpl<>(List.of(product));

        when(productRepository.searchProducts(eq(companyId), eq(asin), any(), any(), eq(pageable)))
                .thenReturn(page);

        // Act
        PageResponse<ProductListResponse> response = productService.searchProducts(companyId, asin, null, null, pageable);

        // Assert
        assertEquals(1, response.getTotalElements());
        assertEquals(1L, response.getContent().get(0).getId());
        verify(keepaService, never()).fetchPriceByAsin(any(), any(), any());
    }

    @Test
    void searchProducts_AsinNotInDb_FetchesFromKeepa() {
        // Arrange
        Long companyId = 1L;
        String asin = "B08H93ZRK9";
        Pageable pageable = PageRequest.of(0, 10);

        when(productRepository.searchProducts(eq(companyId), eq(asin), any(), any(), eq(pageable)))
                .thenReturn(Page.empty());

        when(keepaService.isAvailable(companyId)).thenReturn(true);

        CompetitorPrice price = CompetitorPrice.builder()
                .competitorProductTitle("Keepa Product Title")
                .price(new BigDecimal("100.00"))
                .build();

        when(keepaService.fetchPriceByAsin(eq(asin), any(), eq(companyId)))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(price)));

        // Act
        PageResponse<ProductListResponse> response = productService.searchProducts(companyId, asin, null, null, pageable);

        // Assert
        assertEquals(1, response.getTotalElements());
        assertEquals(-1L, response.getContent().get(0).getId());
        assertEquals("Keepa Product Title", response.getContent().get(0).getName());
        assertEquals(asin, response.getContent().get(0).getSku());

        verify(keepaService).fetchPriceByAsin(eq(asin), any(), eq(companyId));
    }

    @Test
    void searchProducts_AsinNotInDb_KeepaNotFound_ReturnsEmpty() {
        // Arrange
        Long companyId = 1L;
        String asin = "B08H93ZRK9";
        Pageable pageable = PageRequest.of(0, 10);

        when(productRepository.searchProducts(eq(companyId), eq(asin), any(), any(), eq(pageable)))
                .thenReturn(Page.empty());

        when(keepaService.isAvailable(companyId)).thenReturn(true);

        when(keepaService.fetchPriceByAsin(eq(asin), any(), eq(companyId)))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        // Act
        PageResponse<ProductListResponse> response = productService.searchProducts(companyId, asin, null, null, pageable);

        // Assert
        assertEquals(0, response.getTotalElements());
        verify(keepaService).fetchPriceByAsin(eq(asin), any(), eq(companyId));
    }
}
