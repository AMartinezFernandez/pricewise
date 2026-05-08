package com.alvaro.pricewise.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.alvaro.pricewise.config.KeepaConfig;
import com.alvaro.pricewise.entity.CompanyApiKey;
import com.alvaro.pricewise.entity.Competitor;
import com.alvaro.pricewise.entity.CompetitorPrice;
import com.alvaro.pricewise.entity.Product;
import com.alvaro.pricewise.repository.CompetitorPriceRepository;
import com.alvaro.pricewise.repository.CompetitorRepository;
import com.keepa.api.backend.KeepaAPI;
import com.keepa.api.backend.helper.ProductAnalyzer;
import com.keepa.api.backend.structs.AmazonLocale;
import com.keepa.api.backend.structs.Request;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
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
    private final CompanyApiKeyService companyApiKeyService;
    private final Counter keepaRequestsSuccess;
    private final Counter keepaRequestsError;
    private final Timer keepaDuration;

    private final ConcurrentHashMap<Long, KeepaAPI> keepaInstances = new ConcurrentHashMap<>();
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
        initAmazonCompetitor();
        log.info("Keepa Service inicializado (API keys por empresa)");
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
     * Obtiene la instancia de KeepaAPI para una empresa, creándola si no existe.
     * Retorna Optional.empty() si la empresa no tiene API key de Keepa configurada.
     */
    private Optional<KeepaAPI> getKeepaForCompany(Long companyId) {
        KeepaAPI cached = keepaInstances.get(companyId);
        if (cached != null) {
            return Optional.of(cached);
        }
        return companyApiKeyService.getDecryptedKey(companyId, CompanyApiKey.Provider.KEEPA)
                .map(apiKey -> keepaInstances.computeIfAbsent(companyId, id -> new KeepaAPI(apiKey)));
    }

    /**
     * Invalida la instancia cacheada de KeepaAPI para una empresa.
     * Llamar cuando se actualice o elimine la API key.
     */
    public void invalidateCache(Long companyId) {
        keepaInstances.remove(companyId);
    }

    /**
     * Obtiene precio con retry logic y backoff exponencial.
     * Usa la API key de Keepa configurada para la empresa del producto.
     */
    @Async("keepaExecutor")
    public CompletableFuture<Optional<CompetitorPrice>> fetchPriceByAsin(String asin, Product product, Long companyId) {
        Timer.Sample sample = Timer.start();
        return fetchPriceWithRetry(asin, product, companyId, 0)
                .whenComplete((result, ex) -> {
                    sample.stop(keepaDuration);
                    if (ex != null || result == null || result.isEmpty()) {
                        keepaRequestsError.increment();
                    } else {
                        keepaRequestsSuccess.increment();
                    }
                });
    }

    private CompletableFuture<Optional<CompetitorPrice>> fetchPriceWithRetry(String asin, Product product, Long companyId, int attempt) {
        Optional<KeepaAPI> keepaOpt = getKeepaForCompany(companyId);
        if (keepaOpt.isEmpty()) {
            log.warn("Empresa {} no tiene API key de Keepa configurada", companyId);
            return CompletableFuture.completedFuture(Optional.empty());
        }

        if (attempt >= MAX_RETRIES) {
            log.error("Maximo de reintentos alcanzado para ASIN: {}", asin);
            return CompletableFuture.completedFuture(Optional.empty());
        }

        if (attempt > 0) {
            long backoffMs = INITIAL_BACKOFF_MS * (long) Math.pow(2, attempt - 1);
            log.debug("Reintento {} para ASIN {} despues de {}ms", attempt, asin, backoffMs);
            return CompletableFuture.supplyAsync(() -> null,
                    CompletableFuture.delayedExecutor(backoffMs, TimeUnit.MILLISECONDS))
                    .thenCompose(ignored -> executeKeepaRequest(asin, product, companyId, keepaOpt.get(), attempt));
        }

        return executeKeepaRequest(asin, product, companyId, keepaOpt.get(), attempt);
    }

    private CompletableFuture<Optional<CompetitorPrice>> executeKeepaRequest(
            String asin, Product product, Long companyId, KeepaAPI keepaAPI, int attempt) {
        CompletableFuture<Optional<CompetitorPrice>> future = new CompletableFuture<>();

        try {
            if (!rateLimiter.tryAcquire(30, TimeUnit.SECONDS)) {
                log.warn("Timeout en rate limiter para ASIN: {}", asin);
                return fetchPriceWithRetry(asin, product, companyId, attempt + 1);
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
                                            log.info("Precio guardado ASIN {}: {} EUR (empresa {})", asin, saved.getPrice(), companyId);
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
                                fetchPriceWithRetry(asin, product, companyId, attempt + 1)
                                        .thenAccept(future::complete);
                            }
                        } finally {
                            rateLimiter.release();
                        }
                    })
                    .fail(failure -> {
                        rateLimiter.release();
                        log.warn("Fallo Keepa status={}, reintentando...", failure.status);
                        fetchPriceWithRetry(asin, product, companyId, attempt + 1)
                                .thenAccept(future::complete);
                    });

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.complete(Optional.empty());
        } catch (Exception e) {
            rateLimiter.release();
            log.warn("Excepcion Keepa: {}, reintentando...", e.getMessage());
            return fetchPriceWithRetry(asin, product, companyId, attempt + 1);
        }

        return future;
    }

    private CompetitorPrice parseKeepaProduct(com.keepa.api.backend.structs.Product keepaProduct, Product product) {
        try {
            // Orden de prioridad: AMAZON (oferta directa) → NEW (mínimo nuevo) → BUY_BOX_SHIPPING (oferta destacada con envío).
            // El tercer fallback cubre productos vendidos por sellers, donde AMAZON y NEW vienen vacíos.
            int amazonPriceRaw = -1;
            String priceSource = null;
            for (com.keepa.api.backend.structs.Product.CsvType type : java.util.List.of(
                    com.keepa.api.backend.structs.Product.CsvType.AMAZON,
                    com.keepa.api.backend.structs.Product.CsvType.NEW,
                    com.keepa.api.backend.structs.Product.CsvType.BUY_BOX_SHIPPING)) {
                int candidate = ProductAnalyzer.getLast(keepaProduct.csv[type.index], type);
                if (candidate != -1) {
                    amazonPriceRaw = candidate;
                    priceSource = type.name();
                    break;
                }
            }

            if (amazonPriceRaw == -1) {
                return null;
            }
            log.debug("Precio ASIN {} obtenido vía CSV {}", keepaProduct.asin, priceSource);

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

    /**
     * Comprueba si Keepa está disponible para una empresa concreta.
     */
    public boolean isAvailable(Long companyId) {
        return getKeepaForCompany(companyId).isPresent();
    }

    public String getApiStatus(Long companyId) {
        return isAvailable(companyId) ? "READY" : "NO_CONFIGURED";
    }
}
