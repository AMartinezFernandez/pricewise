package com.alvaro.pricewise.controller;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alvaro.pricewise.dto.common.ApiResponse;
import com.alvaro.pricewise.dto.common.PageResponse;

import com.alvaro.pricewise.dto.product.ProductDTOs.CreateProductRequest;
import com.alvaro.pricewise.dto.product.ProductDTOs.ProductListResponse;
import com.alvaro.pricewise.dto.product.ProductDTOs.ProductResponse;
import com.alvaro.pricewise.dto.product.ProductDTOs.UpdateProductRequest;
import com.alvaro.pricewise.security.UserPrincipal;
import com.alvaro.pricewise.service.ProductService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "name", "currentPrice", "costPrice", "category", "brand", "createdAt", "updatedAt");

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateProductRequest request
    ) {
        ProductResponse response = productService.createProduct(
                userPrincipal.requireCompanyId(), userPrincipal.getId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Producto creado exitosamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable @org.springframework.lang.NonNull Long id
    ) {
        ProductResponse response = productService.getProduct(userPrincipal.requireCompanyId(), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductListResponse>>> getProducts(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sortBy, sortDir));
        PageResponse<ProductListResponse> response = productService.getProducts(userPrincipal.requireCompanyId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/monitored")
    public ResponseEntity<ApiResponse<PageResponse<ProductListResponse>>> getMonitoredProducts(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sortBy, sortDir));
        PageResponse<ProductListResponse> response = productService.getMonitoredProducts(userPrincipal.requireCompanyId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private Sort buildSort(String sortBy, String sortDir) {
        String field = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "name";
        return sortDir.equalsIgnoreCase("desc")
                ? Sort.by(field).descending()
                : Sort.by(field).ascending();
    }

    @GetMapping("/search")
    public CompletableFuture<ResponseEntity<ApiResponse<PageResponse<ProductListResponse>>>> searchProducts(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return productService.searchProducts(userPrincipal.requireCompanyId(), name, category, brand, pageable)
                .thenApply(response -> ResponseEntity.ok(ApiResponse.success(response)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable @org.springframework.lang.NonNull Long id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        ProductResponse response = productService.updateProduct(userPrincipal.requireCompanyId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Producto actualizado"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable @org.springframework.lang.NonNull Long id
    ) {
        productService.deleteProduct(userPrincipal.requireCompanyId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Producto eliminado"));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<String> categories = productService.getCategories(userPrincipal.requireCompanyId());
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/brands")
    public ResponseEntity<ApiResponse<List<String>>> getBrands(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<String> brands = productService.getBrands(userPrincipal.requireCompanyId());
        return ResponseEntity.ok(ApiResponse.success(brands));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countProducts(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        long count = productService.countProducts(userPrincipal.requireCompanyId());
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
