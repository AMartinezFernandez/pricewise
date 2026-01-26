package com.alvaro.pricewise.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.alvaro.pricewise.config.KeepaConfig;
import com.alvaro.pricewise.entity.Competitor;
import com.alvaro.pricewise.entity.CompetitorPrice;
import com.alvaro.pricewise.entity.Product;
import com.alvaro.pricewise.repository.CompetitorPriceRepository;
import com.alvaro.pricewise.repository.CompetitorRepository;
import com.keepa.api.backend.KeepaAPI;
import com.keepa.api.backend.helper.ProductAnalyzer;
import com.keepa.api.backend.structs.AmazonLocale;
import com.keepa.api.backend.structs.Request;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio para obtener precios de Amazon via Keepa API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeepaService {

    private final KeepaConfig keepaConfig;
    private final CompetitorRepository competitorRepository;
    private final CompetitorPriceRepository competitorPriceRepository;

    private KeepaAPI keepaAPI;
    private volatile Competitor amazonCompetitor;
    
    // Objeto de bloqueo para sincronización del recurso compartido amazonCompetitor
    private final Object amazonCompetitorLock = new Object();
    
    private Semaphore rateLimiter;
    private static final int MAX_CONCURRENT_REQUESTS = 3;
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;

    @PostConstruct
    public void init() {
        this.rateLimiter = new Semaphore(MAX_CONCURRENT_REQUESTS);
        
        if (keepaConfig.isConfigured()) {
            this.keepaAPI = new KeepaAPI(keepaConfig.getApiKey());
            log.info("Keepa API inicializada para locale: {}", keepaConfig.getDefaultLocale());
            initAmazonCompetitor();
        } else {
            log.warn("Keepa API no configurada. Añade KEEPA_API_KEY en .env");
        }
    }

    /**
     * Inicializa el competidor Amazon de forma thread-safe.
     * Sincronizado para evitar condiciones de carrera durante la inicialización.
     */
    private void initAmazonCompetitor() {
        synchronized (amazonCompetitorLock) {
            if (amazonCompetitor != null) {
                return; // Ya inicializado
            }
            amazonCompetitor = competitorRepository.findByCode("AMAZON_ES")
                    .orElseGet(() -> {
                        Competitor amazon = Competitor.builder()
                                .name("Amazon España")
                                .code("AMAZON_ES")
                                .baseUrl("https://www.amazon.es")
                                .logoUrl("https://logo.clearbit.com/amazon.es")
                                .sourceType(Competitor.SourceType.API)
                                .sourceConfig("keepa")
                                .active(true)
                                .build();
                        log.info("Creando competidor Amazon ES en base de datos");
                        return competitorRepository.save(amazon);
                    });
        }
    }

    /**
     * Obtiene el competidor Amazon de forma thread-safe.
     * Utiliza double-checked locking para optimizar el rendimiento.
     */
    private Competitor getAmazonCompetitor() {
        Competitor localRef = amazonCompetitor;
        if (localRef == null) {
            synchronized (amazonCompetitorLock) {
                localRef = amazonCompetitor;
                if (localRef == null) {
                    initAmazonCompetitor();
                    localRef = amazonCompetitor;
                }
            }
        }
        return localRef;
    }

    /**
     * Obtiene precio con retry logic y backoff exponencial.
     */
    @Async("keepaExecutor")
    public CompletableFuture<Optional<CompetitorPrice>> fetchPriceByAsin(String asin, Product product) {
        return fetchPriceWithRetry(asin, product, 0);
    }

    private CompletableFuture<Optional<CompetitorPrice>> fetchPriceWithRetry(String asin, Product product, int attempt) {
        if (!keepaConfig.isConfigured()) {
            log.error("Keepa API no configurada");
            return CompletableFuture.completedFuture(Optional.empty());
        }

        if (attempt >= MAX_RETRIES) {
            log.error("Maximo de reintentos alcanzado para ASIN: {}", asin);
            return CompletableFuture.completedFuture(Optional.empty());
        }

        if (attempt > 0) {
            long backoffMs = INITIAL_BACKOFF_MS * (long) Math.pow(2, attempt - 1);
            log.debug("Reintento {} para ASIN {} despues de {}ms", attempt, asin, backoffMs);
            try {
                Thread.sleep(backoffMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return CompletableFuture.completedFuture(Optional.empty());
            }
        }

        CompletableFuture<Optional<CompetitorPrice>> future = new CompletableFuture<>();

        try {
            if (!rateLimiter.tryAcquire(30, TimeUnit.SECONDS)) {
                log.warn("Timeout en rate limiter para ASIN: {}", asin);
                return fetchPriceWithRetry(asin, product, attempt + 1);
            }

            AmazonLocale locale = getLocale(keepaConfig.getDefaultLocale());
            Request request = Request.getProductRequest(locale, keepaConfig.getHistoryDays(), null, asin);

            keepaAPI.sendRequest(request)
                    .done(result -> {
                        try {
                            if (result.status.name().equals("OK")) {
                                if (result.products != null && result.products.length > 0) {
                                    CompetitorPrice competitorPrice = parseKeepaProduct(result.products[0], product);
                                    
                                    if (competitorPrice != null) {
                                        if (product.getId() != null) {
                                            CompetitorPrice saved = competitorPriceRepository.save(competitorPrice);
                                            log.info("Precio guardado ASIN {}: {} EUR", asin, saved.getPrice());
                                            future.complete(Optional.of(saved));
                                        } else {
                                            log.info("Precio obtenido ASIN {}: {} EUR", asin, competitorPrice.getPrice());
                                            future.complete(Optional.of(competitorPrice));
                                        }
                                    } else {
                                        log.warn("ASIN {} sin precio disponible", asin);
                                        future.complete(Optional.empty());
                                    }
                                } else {
                                    log.warn("ASIN {} no encontrado", asin);
                                    future.complete(Optional.empty());
                                }
                            } else {
                                log.warn("Error Keepa status={}, reintentando...", result.status);
                                rateLimiter.release();
                                fetchPriceWithRetry(asin, product, attempt + 1)
                                        .thenAccept(future::complete);
                                return;
                            }
                        } finally {
                            rateLimiter.release();
                        }
                    })
                    .fail(failure -> {
                        rateLimiter.release();
                        log.warn("Fallo Keepa status={}, reintentando...", failure.status);
                        fetchPriceWithRetry(asin, product, attempt + 1)
                                .thenAccept(future::complete);
                    });

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.complete(Optional.empty());
        } catch (Exception e) {
            rateLimiter.release();
            log.warn("Excepcion Keepa: {}, reintentando...", e.getMessage());
            return fetchPriceWithRetry(asin, product, attempt + 1);
        }

        return future;
    }

    private CompetitorPrice parseKeepaProduct(com.keepa.api.backend.structs.Product keepaProduct, Product product) {
        try {
            int amazonPriceRaw = ProductAnalyzer.getLast(
                    keepaProduct.csv[com.keepa.api.backend.structs.Product.CsvType.AMAZON.index],
                    com.keepa.api.backend.structs.Product.CsvType.AMAZON
            );

            if (amazonPriceRaw == -1) {
                amazonPriceRaw = ProductAnalyzer.getLast(
                        keepaProduct.csv[com.keepa.api.backend.structs.Product.CsvType.NEW.index],
                        com.keepa.api.backend.structs.Product.CsvType.NEW
                );
            }

            if (amazonPriceRaw == -1) {
                return null;
            }

            BigDecimal price = new BigDecimal(amazonPriceRaw)
                    .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);

            String productUrl = String.format("https://www.amazon.es/dp/%s", keepaProduct.asin);

            return CompetitorPrice.builder()
                    .product(product)
                    .competitor(getAmazonCompetitor()) // Acceso thread-safe al recurso compartido
                    .productUrl(productUrl)
                    .competitorProductTitle(keepaProduct.title)
                    .price(price)
                    .currency("EUR")
                    .available(true)
                    .source("keepa-api")
                    .scrapedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Error parseando Keepa: {}", e.getMessage());
            return null;
        }
    }

    private AmazonLocale getLocale(String code) {
        return switch (code.toUpperCase()) {
            case "ES" -> AmazonLocale.ES;
            case "US" -> AmazonLocale.US;
            case "DE" -> AmazonLocale.DE;
            case "FR" -> AmazonLocale.FR;
            case "UK", "GB" -> AmazonLocale.GB;
            case "IT" -> AmazonLocale.IT;
            case "CA" -> AmazonLocale.CA;
            case "JP" -> AmazonLocale.JP;
            default -> AmazonLocale.ES;
        };
    }

    public boolean isAvailable() {
        return keepaConfig.isConfigured() && keepaAPI != null;
    }

    public String getApiStatus() {
        return isAvailable() ? "READY" : "NO_CONFIGURED";
    }
}
