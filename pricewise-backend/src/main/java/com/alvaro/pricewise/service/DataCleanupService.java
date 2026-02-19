package com.alvaro.pricewise.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.pricewise.repository.CompetitorPriceRepository;
import com.alvaro.pricewise.repository.PriceHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio de limpieza automatica de datos historicos.
 * Ejecuta diariamente para purgar registros antiguos segun TTL configurable.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataCleanupService {

    private final CompetitorPriceRepository competitorPriceRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    @Value("${pricewise.cleanup.competitor-prices-days:365}")
    private int competitorPriceRetentionDays;

    @Value("${pricewise.cleanup.price-history-days:730}")
    private int priceHistoryRetentionDays;

    /**
     * Purga datos historicos antiguos. Ejecuta diariamente a las 3:00 AM.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void purgeOldData() {
        log.info("Iniciando limpieza de datos historicos...");

        LocalDateTime competitorCutoff = LocalDateTime.now().minusDays(competitorPriceRetentionDays);
        competitorPriceRepository.deleteByScrapedAtBefore(competitorCutoff);
        log.info("Precios de competencia anteriores a {} eliminados", competitorCutoff);

        LocalDateTime historyCutoff = LocalDateTime.now().minusDays(priceHistoryRetentionDays);
        priceHistoryRepository.deleteByRecordedAtBefore(historyCutoff);
        log.info("Historial de precios anterior a {} eliminado", historyCutoff);

        log.info("Limpieza de datos historicos completada");
    }
}
