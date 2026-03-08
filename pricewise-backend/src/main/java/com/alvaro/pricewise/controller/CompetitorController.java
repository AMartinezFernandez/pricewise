package com.alvaro.pricewise.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alvaro.pricewise.dto.common.ApiResponse;
import com.alvaro.pricewise.entity.CompetitorPrice;
import com.alvaro.pricewise.entity.Product;
import com.alvaro.pricewise.repository.ProductRepository;
import com.alvaro.pricewise.service.KeepaProductFactory;
import com.alvaro.pricewise.service.KeepaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller para operaciones de monitoreo de precios de competencia.
 * Integra con Keepa API para obtener precios de Amazon.
 */
@Slf4j
@RestController
@RequestMapping("/api/competitors")
@RequiredArgsConstructor
public class CompetitorController {

    private final KeepaService keepaService;
    private final ProductRepository productRepository;

    /**
     * Verifica el estado de la integración con Keepa
     */
    @GetMapping("/status")
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
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'COMPANY_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<AmazonPriceDTO>> getAmazonPrice(@PathVariable String asin) {

        if (!keepaService.isAvailable()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Keepa API no configurada. Añade KEEPA_API_KEY en .env"));
        }

        try {
            Product tempProduct = KeepaProductFactory.createTemporaryProduct(asin);

            // Consultar Keepa (con timeout de 30 segundos)
            Optional<CompetitorPrice> result = keepaService.fetchPriceByAsin(asin, tempProduct)
                    .get(30, TimeUnit.SECONDS);

            if (result.isPresent()) {
                CompetitorPrice cp = result.get();
                AmazonPriceDTO dto = new AmazonPriceDTO(
                        asin,
                        cp.getCompetitorProductTitle(),
                        cp.getCompetitorProductTitle(),
                        cp.getPrice(),
                        cp.getCurrency(),
                        cp.isAvailable(),
                        cp.getProductUrl(),
                        cp.getScrapedAt() != null ? cp.getScrapedAt().toString() : null,
                        cp.getSource()
                );

                return ResponseEntity.ok(ApiResponse.success(dto, "Precio obtenido correctamente"));
            } else {
                return ResponseEntity.ok(ApiResponse.error(
                        "No se encontró precio para el ASIN: " + asin + ". Puede que el producto no exista o no tenga precio disponible."));
            }

        } catch (InterruptedException | java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
            log.error("Error consultando precio de Amazon para ASIN {}: {}", asin, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error al consultar Keepa: " + e.getMessage()));
        }
    }

    /**
     * DTO tipado para la respuesta de precio de Amazon.
     * Garantiza serialización correcta (scrapedAt como String ISO, price como BigDecimal).
     */
    public record AmazonPriceDTO(
            String asin,
            String title,
            String competitorProductTitle,
            BigDecimal price,
            String currency,
            boolean available,
            String productUrl,
            String scrapedAt,
            String source
    ) {}

    /**
     * Busca el precio de un producto de nuestra BD en Amazon.
     * Usa el campo ASIN del producto para la consulta.
     */
    @PostMapping("/amazon/sync/{productId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'COMPANY_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<AmazonPriceDTO>> syncProductWithAmazon(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.alvaro.pricewise.security.UserPrincipal userPrincipal,
            @PathVariable @org.springframework.lang.NonNull Long productId,
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

        // Validar multi-tenancy
        if (userPrincipal.requireCompanyId() != null && !product.getCompany().getId().equals(userPrincipal.requireCompanyId())) {
            return ResponseEntity.notFound().build();
        }

        // Si no se proporciona ASIN, usar el campo asin del producto
        String searchAsin = asin != null ? asin : product.getAsin();
        if (searchAsin == null || searchAsin.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Proporciona un ASIN o asegúrate de que el producto tenga ASIN"));
        }

        try {
            Optional<CompetitorPrice> result = keepaService.fetchPriceByAsin(searchAsin, product)
                    .get(30, TimeUnit.SECONDS);

            if (result.isPresent()) {
                CompetitorPrice cp = result.get();
                AmazonPriceDTO dto = new AmazonPriceDTO(
                        searchAsin,
                        cp.getCompetitorProductTitle(),
                        cp.getCompetitorProductTitle(),
                        cp.getPrice(),
                        cp.getCurrency(),
                        cp.isAvailable(),
                        cp.getProductUrl(),
                        cp.getScrapedAt() != null ? cp.getScrapedAt().toString() : null,
                        cp.getSource()
                );
                return ResponseEntity.ok(ApiResponse.success(dto, "Precio sincronizado correctamente"));
            } else {
                return ResponseEntity.ok(ApiResponse.error(
                        "No se encontró precio en Amazon para este producto"));
            }

        } catch (InterruptedException | java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
            log.error("Error sincronizando producto {} con Amazon: {}", productId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error al sincronizar: " + e.getMessage()));
        }
    }
}
