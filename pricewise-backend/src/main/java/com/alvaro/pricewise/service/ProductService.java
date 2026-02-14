package com.alvaro.pricewise.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
import com.alvaro.pricewise.repository.CompanyRepository;
import com.alvaro.pricewise.repository.PriceHistoryRepository;
import com.alvaro.pricewise.repository.ProductRepository;
import com.alvaro.pricewise.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final KeepaService keepaService;

    // Regex para validar formato ASIN (10 caracteres alfanuméricos que empiezan por B)
    private static final String ASIN_PATTERN = "^[B0-9][A-Z0-9]{9}$";

    @Transactional
    @CacheEvict(value = {"categories", "brands"}, key = "#companyId")
    public ProductResponse createProduct(Long companyId, Long userId, CreateProductRequest request) {
        log.debug("Creando producto para empresa: {}, usuario: {}", companyId, userId);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Validar SKU único si se proporciona
        if (request.getSku() != null && !request.getSku().isBlank()) {
            productRepository.findBySku(request.getSku())
                    .ifPresent(p -> {
                        throw new BadRequestException("Ya existe un producto con el SKU: " + request.getSku());
                    });
        }

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .sku(request.getSku())
                .ean(request.getEan())
                .currentPrice(request.getCurrentPrice())
                .costPrice(request.getCostPrice())
                .category(request.getCategory())
                .brand(request.getBrand())
                .imageUrl(request.getImageUrl())
                .monitoringEnabled(request.getMonitoringEnabled() != null ? request.getMonitoringEnabled() : true)
                .stockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0)
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
    public ProductResponse getProduct(Long companyId, Long productId) {
        Product product = findProductByCompanyAndId(companyId, productId);
        return ProductResponse.fromEntity(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductListResponse> getProducts(Long companyId, Pageable pageable) {
        Page<Product> page = productRepository.findByCompanyIdAndActiveTrue(companyId, pageable);
        List<ProductListResponse> content = page.getContent().stream()
                .map(ProductListResponse::fromEntity)
                .toList();
        return PageResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductListResponse> searchProducts(
            Long companyId,
            String name,
            String category,
            String brand,
            Pageable pageable
    ) {
        Page<Product> page = productRepository.searchProducts(companyId, name, category, brand, pageable);
        
        // Si no hay resultados y la búsqueda parece un ASIN, intentar buscar en Keepa
        if (page.isEmpty() && name != null && name.matches(ASIN_PATTERN)) {
            log.info("Búsqueda de producto vacía, intentando buscar ASIN en Keepa: {}", name);
            return searchInKeepa(name, pageable);
        }

        List<ProductListResponse> content = page.getContent().stream()
                .map(ProductListResponse::fromEntity)
                .toList();
        return PageResponse.from(page, content);
    }

    private PageResponse<ProductListResponse> searchInKeepa(String asin, Pageable pageable) {
        if (!keepaService.isAvailable()) {
            return PageResponse.<ProductListResponse>builder()
                    .content(List.of())
                    .totalElements(0)
                    .totalPages(0)
                    .build();
        }

        try {
            Product tempProduct = Product.builder()
                    .name("Consulta temporal - " + asin)
                    .currentPrice(BigDecimal.ZERO)
                    .build();

            return keepaService.fetchPriceByAsin(asin, tempProduct)
                    .thenApply(optPrice -> optPrice.map(price -> {
                        ProductListResponse response = ProductListResponse.builder()
                                .id(null)
                                .name(price.getCompetitorProductTitle())
                                .sku(asin)
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
    @CacheEvict(value = {"categories", "brands"}, key = "#companyId")
    public ProductResponse updateProduct(Long companyId, Long productId, UpdateProductRequest request) {
        log.debug("Actualizando producto: {} para empresa: {}", productId, companyId);

        Product product = findProductByCompanyAndId(companyId, productId);
        BigDecimal oldPrice = product.getCurrentPrice();

        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getSku() != null) {
            productRepository.findBySku(request.getSku())
                    .filter(p -> !p.getId().equals(productId))
                    .ifPresent(p -> {
                        throw new BadRequestException("Ya existe otro producto con el SKU: " + request.getSku());
                    });
            product.setSku(request.getSku());
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
    public void deleteProduct(Long companyId, Long productId) {
        log.info("Eliminando producto: {} para empresa: {}", productId, companyId);
        
        Product product = findProductByCompanyAndId(companyId, productId);
        product.setActive(false);
        productRepository.save(product);
        
        log.info("Producto desactivado: {}", productId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "#companyId")
    public List<String> getCategories(Long companyId) {
        return productRepository.findDistinctCategoriesByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "brands", key = "#companyId")
    public List<String> getBrands(Long companyId) {
        return productRepository.findDistinctBrandsByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public long countProducts(Long companyId) {
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
