package com.alvaro.pricewise.scheduler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.alvaro.pricewise.entity.Product;
import com.alvaro.pricewise.repository.ProductRepository;
import com.alvaro.pricewise.service.KeepaService;
import com.alvaro.pricewise.service.PriceAnalysisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Job de Quartz para actualizacion periodica de precios y analisis.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceMonitorJob implements Job {

    private final ProductRepository productRepository;
    private final KeepaService keepaService;
    private final PriceAnalysisService priceAnalysisService;
    
    private static final int PAGE_SIZE = 50;
    private static final long DELAY_BETWEEN_PAGES_MS = 1000;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("Iniciando Job de Monitoreo de Precios");

        Set<Long> companyIds = new HashSet<>();

        try {
            companyIds = processProductsInBatches();
            log.info("Actualizacion de precios finalizada");
            
            // Ejecutar analisis para cada empresa afectada
            for (Long companyId : companyIds) {
                try {
                    priceAnalysisService.analyzeAllProductsForUser(companyId);
                    log.debug("Analisis completado para empresa {}", companyId);
                } catch (Exception e) {
                    log.error("Error en analisis para empresa {}: {}", companyId, e.getMessage());
                }
            }
            
            log.info("Job de Monitoreo finalizado. Empresas analizadas: {}", companyIds.size());
        } catch (Exception e) {
            log.error("Error en PriceMonitorJob: {}", e.getMessage(), e);
        }
    }

    private Set<Long> processProductsInBatches() {
        Set<Long> companyIds = new HashSet<>();
        int page = 0;
        Page<Product> productPage;

        do {
            productPage = productRepository.findMonitoredProductsWithCompany(
                    PageRequest.of(page, PAGE_SIZE)
            );

            List<Product> trackableProducts = productPage.getContent().stream()
                    .filter(p -> p.getAsin() != null && !p.getAsin().isBlank())
                    .toList();

            // Filtrar productos cuya empresa tenga API key de Keepa
            List<Product> productsWithKey = trackableProducts.stream()
                    .filter(p -> keepaService.isAvailable(p.getCompany().getId()))
                    .toList();

            if (!productsWithKey.isEmpty()) {
                log.debug("Procesando lote {}: {} productos ({} sin API key omitidos)",
                        page, productsWithKey.size(), trackableProducts.size() - productsWithKey.size());
                processBatch(productsWithKey);

                // Guardar company IDs para analisis posterior
                productsWithKey.forEach(p -> companyIds.add(p.getCompany().getId()));
            }

            page++;
            
            if (productPage.hasNext()) {
                sleepBetweenBatches();
            }
        } while (productPage.hasNext());

        return companyIds;
    }

    private void processBatch(List<Product> products) {
        List<CompletableFuture<Void>> futures = products.stream()
                .map(this::updateProductPrice)
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private CompletableFuture<Void> updateProductPrice(Product product) {
        String asin = product.getAsin();
        if (asin == null || asin.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }

        return keepaService.fetchPriceByAsin(asin, product, product.getCompany().getId())
                .thenAccept(resultOpt -> {
                    if (resultOpt.isPresent()) {
                        log.debug("Precio actualizado para ASIN {}", asin);
                    } else {
                        log.debug("Sin precio para ASIN {}", asin);
                    }
                })
                .exceptionally(ex -> {
                    log.error("Error actualizando ASIN {}: {}", asin, ex.getMessage());
                    return null;
                });
    }

    private void sleepBetweenBatches() {
        try {
            Thread.sleep(DELAY_BETWEEN_PAGES_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Sleep interrumpido entre lotes");
        }
    }
}
