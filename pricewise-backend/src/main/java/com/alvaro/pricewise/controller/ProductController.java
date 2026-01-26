package com.alvaro.pricewise.controller;

import com.alvaro.pricewise.dto.common.ApiResponse;
import com.alvaro.pricewise.dto.common.PageResponse;
import com.alvaro.pricewise.dto.product.ProductDTOs.*;
import com.alvaro.pricewise.security.UserPrincipal;
import com.alvaro.pricewise.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Productos", description = "CRUD de productos del usuario")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "Crear producto", description = "Crea un nuevo producto para el usuario autenticado")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateProductRequest request
    ) {
        ProductResponse response = productService.createProduct(userPrincipal.getId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Producto creado exitosamente"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto", description = "Obtiene un producto por su ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        ProductResponse response = productService.getProduct(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Listar productos", description = "Lista todos los productos del usuario con paginación")
    public ResponseEntity<ApiResponse<PageResponse<ProductListResponse>>> getProducts(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "Número de página (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Campo por el que ordenar")
            @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Dirección del ordenamiento")
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<ProductListResponse> response = productService.getProducts(userPrincipal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar productos", description = "Busca productos con filtros opcionales")
    public ResponseEntity<ApiResponse<PageResponse<ProductListResponse>>> searchProducts(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "Nombre del producto (búsqueda parcial)")
            @RequestParam(required = false) String name,
            @Parameter(description = "Categoría exacta")
            @RequestParam(required = false) String category,
            @Parameter(description = "Marca exacta")
            @RequestParam(required = false) String brand,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        PageResponse<ProductListResponse> response = productService.searchProducts(
                userPrincipal.getId(), name, category, brand, pageable
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto", description = "Actualiza un producto existente")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        ProductResponse response = productService.updateProduct(userPrincipal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Producto actualizado"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar producto", description = "Desactiva un producto (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        productService.deleteProduct(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Producto eliminado"));
    }

    @GetMapping("/categories")
    @Operation(summary = "Listar categorías", description = "Obtiene las categorías únicas del usuario")
    public ResponseEntity<ApiResponse<List<String>>> getCategories(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<String> categories = productService.getCategories(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/brands")
    @Operation(summary = "Listar marcas", description = "Obtiene las marcas únicas del usuario")
    public ResponseEntity<ApiResponse<List<String>>> getBrands(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<String> brands = productService.getBrands(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(brands));
    }

    @GetMapping("/count")
    @Operation(summary = "Contar productos", description = "Obtiene el total de productos del usuario")
    public ResponseEntity<ApiResponse<Long>> countProducts(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        long count = productService.countProducts(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
