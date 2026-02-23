package com.alvaro.pricewise.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.pricewise.dto.common.PageResponse;
import com.alvaro.pricewise.dto.product.ProductDTOs.CreateProductRequest;
import com.alvaro.pricewise.dto.product.ProductDTOs.ProductListResponse;
import com.alvaro.pricewise.dto.product.ProductDTOs.ProductResponse;
import com.alvaro.pricewise.dto.product.ProductDTOs.UpdateProductRequest;
import com.alvaro.pricewise.entity.Company;
import com.alvaro.pricewise.entity.PriceHistory;
import com.alvaro.pricewise.entity.Product;
import com.alvaro.pricewise.entity.User;
import com.alvaro.pricewise.exception.BadRequestException;
import com.alvaro.pricewise.exception.ResourceNotFoundException;
import com.alvaro.pricewise.entity.CompetitorPrice;
import com.alvaro.pricewise.repository.CompanyRepository;
import com.alvaro.pricewise.repository.CompetitorPriceRepository;
import com.alvaro.pricewise.repository.PriceHistoryRepository;
import com.alvaro.pricewise.repository.ProductRepository;
import com.alvaro.pricewise.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final CompetitorPriceRepository competitorPriceRepository;
    private final KeepaService keepaService;

    // Regex para validar formato ASIN (10 caracteres alfanuméricos que empiezan por B)
    private static final String ASIN_PATTERN = "^[B0-9][A-Z0-9]{9}$";

    @Transactional
    public ProductResponse createProduct(@org.springframework.lang.NonNull Long companyId, @org.springframework.lang.NonNull Long userId, CreateProductRequest request) {
        log.debug("Creando producto para empresa: {}, usuario: {}", companyId, userId);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Determinar ASIN: puede venir en campo asin o en sku (compatibilidad)
        String asin = request.getAsin();
        if ((asin == null || asin.isBlank()) && request.getSku() != null && !request.getSku().isBlank()) {
            asin = request.getSku();
        }

        // Validar ASIN único dentro de la misma empresa
        if (asin != null && !asin.isBlank()) {
            final String asinFinal = asin;
            productRepository.findBySkuAndCompanyIdAndActiveTrue(asin, companyId)
                    .ifPresent(p -> {
                        throw new BadRequestException("Ya existe un producto con el ASIN: " + asinFinal);
                    });
        }

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .sku(asin)
                .asin(asin)
                .ean(request.getEan())
                .currentPrice(request.getCurrentPrice())
                .costPrice(request.getCostPrice())
                .minMargin(request.getMinMargin() != null ? request.getMinMargin() : new BigDecimal("0.10"))
                .category(request.getCategory())

                .brand(request.getBrand())
                .imageUrl(request.getImageUrl())
                .monitoringEnabled(java.util.Optional.ofNullable(request.getMonitoringEnabled()).orElse(true))
                .stockQuantity(java.util.Optional.ofNullable(request.getStockQuantity()).orElse(0))
                .active(true)
                .company(company)
                .createdBy(user)
                .build();

        product = productRepository.save(product);

        // Crear registro inicial de precio
        createPriceHistoryEntry(product, null, PriceHistory.ChangeType.INITIAL, "Precio inicial");

        log.info("Producto creado: {} (ID: {})", product.getName(), product.getId());
        return ProductResponse.fromEntity(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(@org.springframework.lang.NonNull Long companyId, @org.springframework.lang.NonNull Long productId) {
        Product product = productRepository.findByCompanyIdAndIdWithCreatedBy(companyId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        ProductResponse response = ProductResponse.fromEntity(product);

        // Enriquecer con el último precio de Amazon persistido
        competitorPriceRepository.findTopByProductIdOrderByScrapedAtDesc(productId)
                .ifPresent(cp -> {
                    response.setAmazonPrice(cp.getPrice());
                    response.setAmazonProductTitle(cp.getCompetitorProductTitle());
                    response.setAmazonPriceUpdatedAt(cp.getScrapedAt());
                });

        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductListResponse> getProducts(@org.springframework.lang.NonNull Long companyId, Pageable pageable) {
        Page<Product> page = productRepository.findByCompanyIdAndActiveTrue(companyId, pageable);
        List<ProductListResponse> content = page.getContent().stream()
                .map(ProductListResponse::fromEntity)
                .toList();
        return PageResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductListResponse> getMonitoredProducts(@org.springframework.lang.NonNull Long companyId, Pageable pageable) {
        Page<Product> page = productRepository.findByCompanyIdAndMonitoringEnabledTrueAndActiveTrue(companyId, pageable);
        List<ProductListResponse> content = page.getContent().stream()
                .map(ProductListResponse::fromEntity)
                .toList();
        return PageResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductListResponse> searchProducts(
            @org.springframework.lang.NonNull Long companyId,
            String name,
            String category,
            String brand,
            Pageable pageable
    ) {
        String searchTerm = name != null ? name.trim() : null;
        Page<Product> page = productRepository.searchProducts(companyId, searchTerm, category, brand, pageable);
        
        // Si no hay resultados y la búsqueda parece un ASIN y no es un producto propio, intentar buscar en Keepa
        // (La búsqueda local ya incluye ASIN en el repositorio modificado)
        if (page.isEmpty() && searchTerm != null && searchTerm.matches(ASIN_PATTERN)) {
            log.info("Búsqueda de producto vacía, intentando buscar ASIN en Keepa: {}", searchTerm);
            return searchInKeepa(searchTerm, pageable);
        }

        List<ProductListResponse> content = page.getContent().stream()
                .map(ProductListResponse::fromEntity)
                .toList();
        return PageResponse.from(page, content);
    }

    private PageResponse<ProductListResponse> searchInKeepa(String asin, Pageable pageable) {
        if (!keepaService.isAvailable()) {
            log.warn("Se intentó buscar en Keepa pero el servicio no está disponible (API Key no configurada)");
            return PageResponse.<ProductListResponse>builder()
                    .content(List.of())
                    .totalElements(0)
                    .totalPages(0)
                    .build();
        }

        try {
            Product tempProduct = KeepaProductFactory.createTemporaryProduct(asin);

            return keepaService.fetchPriceByAsin(asin, tempProduct)
                    .thenApply(optPrice -> optPrice.map(price -> {
                        ProductListResponse response = ProductListResponse.builder()
                                .id(-1L)
                                .name(price.getCompetitorProductTitle())
                                .sku(asin)
                                .asin(asin)
                                .currentPrice(price.getPrice())
                                .category("Amazon Import")
                                .brand("Amazon")
                                .monitoringEnabled(false)
                                .build();
                        
                        return PageResponse.<ProductListResponse>builder()
                                .content(List.of(response))
                                .pageNumber(0)
                                .pageSize(pageable.getPageSize())
                                .totalElements(1)
                                .totalPages(1)
                                .first(true)
                                .last(true)
                                .build();
                    }).orElseGet(() -> PageResponse.<ProductListResponse>builder()
                            .content(List.of())
                            .totalElements(0)
                            .totalPages(0)
                            .build()))
                    .join();

        } catch (Exception e) {
            log.error("Error buscando ASIN {} en Keepa: {}", asin, e.getMessage());
            return PageResponse.<ProductListResponse>builder()
                    .content(List.of())
                    .totalElements(0)
                    .totalPages(0)
                    .build();
        }
    }

    @Transactional
    public ProductResponse updateProduct(@org.springframework.lang.NonNull Long companyId, @org.springframework.lang.NonNull Long productId, UpdateProductRequest request) {
        log.debug("Actualizando producto: {} para empresa: {}", productId, companyId);

        Product product = findProductByCompanyAndId(companyId, productId);
        BigDecimal oldPrice = product.getCurrentPrice();

        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        // ASIN: puede venir en campo asin o sku (compatibilidad)
        String newAsin = request.getAsin() != null ? request.getAsin() : request.getSku();
        if (newAsin != null) {
            productRepository.findBySkuAndCompanyIdAndActiveTrue(newAsin, companyId)
                    .filter(p -> !p.getId().equals(productId))
                    .ifPresent(p -> {
                        throw new BadRequestException("Ya existe otro producto con el ASIN: " + newAsin);
                    });
            product.setSku(newAsin);
            product.setAsin(newAsin);
        }
        if (request.getEan() != null) product.setEan(request.getEan());
        if (request.getCurrentPrice() != null) {
            product.setCurrentPrice(request.getCurrentPrice());
            if (oldPrice.compareTo(request.getCurrentPrice()) != 0) {
                PriceHistory.ChangeType changeType = request.getCurrentPrice().compareTo(oldPrice) > 0
                        ? PriceHistory.ChangeType.INCREASE
                        : PriceHistory.ChangeType.DECREASE;
                createPriceHistoryEntry(product, oldPrice, changeType, "Actualización manual");
            }
        }
        if (request.getCostPrice() != null) product.setCostPrice(request.getCostPrice());
        if (request.getMinMargin() != null) product.setMinMargin(request.getMinMargin());
        if (request.getCategory() != null) product.setCategory(request.getCategory());

        if (request.getBrand() != null) product.setBrand(request.getBrand());
        if (request.getImageUrl() != null) product.setImageUrl(request.getImageUrl());
        if (request.getMonitoringEnabled() != null) product.setMonitoringEnabled(request.getMonitoringEnabled());
        if (request.getActive() != null) product.setActive(request.getActive());
        if (request.getStockQuantity() != null) product.setStockQuantity(request.getStockQuantity());

        product = productRepository.save(product);
        log.info("Producto actualizado: {}", productId);

        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public void deleteProduct(@org.springframework.lang.NonNull Long companyId, @org.springframework.lang.NonNull Long productId) {
        log.info("Eliminando producto: {} para empresa: {}", productId, companyId);
        
        Product product = findProductByCompanyAndId(companyId, productId);
        product.setActive(false);
        productRepository.save(product);
        
        log.info("Producto desactivado: {}", productId);
    }

    @Transactional(readOnly = true)
    public List<String> getCategories(@org.springframework.lang.NonNull Long companyId) {
        return productRepository.findDistinctCategoriesByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public List<String> getBrands(@org.springframework.lang.NonNull Long companyId) {
        return productRepository.findDistinctBrandsByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public long countProducts(@org.springframework.lang.NonNull Long companyId) {
        return productRepository.countByCompanyIdAndActiveTrue(companyId);
    }

    // --- Métodos privados ---

    private Product findProductByCompanyAndId(Long companyId, Long productId) {
        return productRepository.findById(productId)
                .filter(p -> p.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
    }

    private void createPriceHistoryEntry(Product product, BigDecimal previousPrice,
                                         PriceHistory.ChangeType changeType, String reason) {
        PriceHistory history = PriceHistory.builder()
                .product(product)
                .price(product.getCurrentPrice())
                .previousPrice(previousPrice)
                .changeType(changeType)
                .changeReason(reason)
                .build();
        priceHistoryRepository.save(history);
    }
}
