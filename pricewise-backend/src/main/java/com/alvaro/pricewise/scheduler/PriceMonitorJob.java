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

        if (!keepaService.isAvailable()) {
            log.warn("Keepa API no configurada. Saltando ejecucion");
            return;
        }

        Set<Long> userIds = new HashSet<>();

        try {
            userIds = processProductsInBatches();
            log.info("Actualizacion de precios finalizada");
            
            // Ejecutar analisis para cada usuario afectado
            for (Long userId : userIds) {
                try {
                    priceAnalysisService.analyzeAllProductsForUser(userId);
                    log.debug("Analisis completado para usuario {}", userId);
                } catch (Exception e) {
                    log.error("Error en analisis para usuario {}: {}", userId, e.getMessage());
                }
            }
            
            log.info("Job de Monitoreo finalizado. Usuarios analizados: {}", userIds.size());
        } catch (Exception e) {
            log.error("Error en PriceMonitorJob: {}", e.getMessage(), e);
        }
    }

    private Set<Long> processProductsInBatches() {
        Set<Long> userIds = new HashSet<>();
        int page = 0;
        Page<Product> productPage;

        do {
            productPage = productRepository.findByMonitoringEnabledTrueAndActiveTrue(
                    PageRequest.of(page, PAGE_SIZE)
            );

            List<Product> trackableProducts = productPage.getContent().stream()
                    .filter(p -> p.getSku() != null && p.getSku().startsWith("B0"))
                    .toList();

            if (!trackableProducts.isEmpty()) {
                log.debug("Procesando lote {}: {} productos", page, trackableProducts.size());
                processBatch(trackableProducts);
                
                // Guardar user IDs para analisis posterior
                trackableProducts.forEach(p -> userIds.add(p.getUser().getId()));
            }

            page++;
            
            if (productPage.hasNext()) {
                sleepBetweenBatches();
            }
        } while (productPage.hasNext());

        return userIds;
    }

    private void processBatch(List<Product> products) {
        List<CompletableFuture<Void>> futures = products.stream()
                .map(this::updateProductPrice)
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private CompletableFuture<Void> updateProductPrice(Product product) {
        return keepaService.fetchPriceByAsin(product.getSku(), product)
                .thenAccept(resultOpt -> {
                    if (resultOpt.isPresent()) {
                        log.debug("Precio actualizado para {}", product.getSku());
                    } else {
                        log.debug("Sin precio para {}", product.getSku());
                    }
                })
                .exceptionally(ex -> {
                    log.error("Error actualizando {}: {}", product.getSku(), ex.getMessage());
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
