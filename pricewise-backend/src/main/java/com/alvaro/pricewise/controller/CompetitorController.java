package com.alvaro.pricewise.controller;

import com.alvaro.pricewise.dto.common.ApiResponse;
import com.alvaro.pricewise.entity.CompetitorPrice;
import com.alvaro.pricewise.entity.Product;
import com.alvaro.pricewise.repository.ProductRepository;
import com.alvaro.pricewise.service.KeepaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Controller para operaciones de monitoreo de precios de competencia.
 * Integra con Keepa API para obtener precios de Amazon.
 */
@Slf4j
@RestController
@RequestMapping("/api/competitors")
@RequiredArgsConstructor
@Tag(name = "Competidores", description = "Monitoreo de precios de la competencia")
public class CompetitorController {

    private final KeepaService keepaService;
    private final ProductRepository productRepository;

    /**
     * Verifica el estado de la integración con Keepa
     */
    @GetMapping("/status")
    @Operation(summary = "Estado de la API de Keepa", 
               description = "Verifica si la API de Keepa está configurada y disponible")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getKeepaStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("keepaAvailable", keepaService.isAvailable());
        status.put("status", keepaService.getApiStatus());
        status.put("timestamp", LocalDateTime.now());
        
        if (!keepaService.isAvailable()) {
            status.put("message", "Keepa API no configurada. Añade KEEPA_API_KEY en .env");
        } else {
            status.put("message", "Keepa API lista para consultas");
        }
        
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * Busca el precio de un producto en Amazon por su ASIN.
     * Este endpoint es para pruebas - no requiere tener el producto en nuestra BD.
     */
    @GetMapping("/amazon/price/{asin}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Obtener precio de Amazon por ASIN",
               description = "Consulta el precio actual de un producto en Amazon usando Keepa API")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAmazonPrice(@PathVariable String asin) {
        
        if (!keepaService.isAvailable()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Keepa API no configurada. Añade KEEPA_API_KEY en .env"));
        }

        try {
            // Crear un producto temporal para la consulta
            Product tempProduct = Product.builder()
                    .name("Consulta temporal - " + asin)
                    .currentPrice(BigDecimal.ZERO)
                    .build();

            // Consultar Keepa (con timeout de 30 segundos)
            Optional<CompetitorPrice> result = keepaService.fetchPriceByAsin(asin, tempProduct)
                    .get(30, TimeUnit.SECONDS);

            if (result.isPresent()) {
                CompetitorPrice price = result.get();
                Map<String, Object> data = new HashMap<>();
                data.put("asin", asin);
                data.put("title", price.getCompetitorProductTitle());
                data.put("price", price.getPrice());
                data.put("currency", price.getCurrency());
                data.put("available", price.isAvailable());
                data.put("productUrl", price.getProductUrl());
                data.put("scrapedAt", price.getScrapedAt());
                data.put("source", price.getSource());
                
                return ResponseEntity.ok(ApiResponse.success(
                        data, "Precio obtenido correctamente"));
            } else {
                return ResponseEntity.ok(ApiResponse.error(
                        "No se encontró precio para el ASIN: " + asin + ". Puede que el producto no exista o no tenga precio disponible."));
            }

        } catch (Exception e) {
            log.error("Error consultando precio de Amazon para ASIN {}: {}", asin, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error al consultar Keepa: " + e.getMessage()));
        }
    }

    /**
     * Busca el precio de un producto de nuestra BD en Amazon.
     * Usa el campo EAN o SKU para buscar el ASIN correspondiente.
     */
    @PostMapping("/amazon/sync/{productId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Sincronizar precio de producto con Amazon",
               description = "Busca y guarda el precio de Amazon para un producto existente")
    public ResponseEntity<ApiResponse<CompetitorPrice>> syncProductWithAmazon(
            @PathVariable Long productId,
            @RequestParam(required = false) String asin) {
        
        if (!keepaService.isAvailable()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Keepa API no configurada"));
        }

        // Buscar el producto
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Product product = productOpt.get();
        
        // Si no se proporciona ASIN, intentar usar el SKU del producto
        String searchAsin = asin != null ? asin : product.getSku();
        if (searchAsin == null || searchAsin.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Proporciona un ASIN o asegúrate de que el producto tenga SKU"));
        }

        try {
            Optional<CompetitorPrice> result = keepaService.fetchPriceByAsin(searchAsin, product)
                    .get(30, TimeUnit.SECONDS);

            if (result.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(
                        result.get(), "Precio sincronizado correctamente"));
            } else {
                return ResponseEntity.ok(ApiResponse.error(
                        "No se encontró precio en Amazon para este producto"));
            }

        } catch (Exception e) {
            log.error("Error sincronizando producto {} con Amazon: {}", productId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error al sincronizar: " + e.getMessage()));
        }
    }
}
