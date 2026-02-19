package com.alvaro.pricewise.service;

import com.alvaro.pricewise.repository.CompetitorPriceRepository;
import com.alvaro.pricewise.repository.PriceHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataCleanupService Tests")
class DataCleanupServiceTest {

    @Mock
    private CompetitorPriceRepository competitorPriceRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @InjectMocks
    private DataCleanupService dataCleanupService;

    @Test
    @DisplayName("purgeOldData elimina precios de competencia y historial antiguos")
    void purgeOldData_DeletesOldRecords() {
        ReflectionTestUtils.setField(dataCleanupService, "competitorPriceRetentionDays", 365);
        ReflectionTestUtils.setField(dataCleanupService, "priceHistoryRetentionDays", 730);

        dataCleanupService.purgeOldData();

        verify(competitorPriceRepository).deleteByScrapedAtBefore(any(LocalDateTime.class));
        verify(priceHistoryRepository).deleteByRecordedAtBefore(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("purgeOldData respeta los dias de retencion configurados")
    void purgeOldData_RespectsRetentionDays() {
        ReflectionTestUtils.setField(dataCleanupService, "competitorPriceRetentionDays", 30);
        ReflectionTestUtils.setField(dataCleanupService, "priceHistoryRetentionDays", 60);

        dataCleanupService.purgeOldData();

        verify(competitorPriceRepository).deleteByScrapedAtBefore(any(LocalDateTime.class));
        verify(priceHistoryRepository).deleteByRecordedAtBefore(any(LocalDateTime.class));
    }
}
