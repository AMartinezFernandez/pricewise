package com.alvaro.pricewise.service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.alvaro.pricewise.config.KeepaConfig;
import com.alvaro.pricewise.entity.CompanyApiKey;
import com.alvaro.pricewise.repository.CompetitorPriceRepository;
import com.alvaro.pricewise.repository.CompetitorRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.Optional;
import static org.mockito.Mockito.when;

/**
 * Tests para KeepaService enfocados en concurrencia y thread-safety.
 * Usa ReflectionTestUtils para evitar problemas de Mockito con Java 25.
 */
@ExtendWith(MockitoExtension.class)
class KeepaServiceThreadSafetyTest {

    @Mock
    private CompetitorRepository competitorRepository;

    @Mock
    private CompetitorPriceRepository competitorPriceRepository;

    @Mock
    private CompanyApiKeyService companyApiKeyService;

    private KeepaService keepaService;
    private KeepaConfig keepaConfig;

    private static final Long COMPANY_ID = 1L;

    @BeforeEach
    void setUp() {
        keepaConfig = new KeepaConfig();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Counter successCounter = Counter.builder("pricewise.keepa.requests").tag("result", "success").register(registry);
        Counter errorCounter = Counter.builder("pricewise.keepa.requests").tag("result", "error").register(registry);
        Timer timer = Timer.builder("pricewise.keepa.duration").register(registry);
        keepaService = new KeepaService(keepaConfig, competitorRepository, competitorPriceRepository,
                companyApiKeyService, successCounter, errorCounter, timer);
    }

    @Test
    @DisplayName("Debe retornar no disponible cuando empresa no tiene API key")
    void isAvailable_shouldReturnFalse_whenNoApiKey() {
        // Arrange
        when(companyApiKeyService.getDecryptedKey(COMPANY_ID, CompanyApiKey.Provider.KEEPA))
                .thenReturn(Optional.empty());
        keepaService.init();

        // Act & Assert
        assertFalse(keepaService.isAvailable(COMPANY_ID));
        assertEquals("NO_CONFIGURED", keepaService.getApiStatus(COMPANY_ID));
    }

    @Test
    @DisplayName("getApiStatus debe ser thread-safe con múltiples llamadas concurrentes")
    void getApiStatus_shouldBeThreadSafe() throws InterruptedException {
        // Arrange
        when(companyApiKeyService.getDecryptedKey(COMPANY_ID, CompanyApiKey.Provider.KEEPA))
                .thenReturn(Optional.empty());
        keepaService.init();

        int threadCount = 50;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Act
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String status = keepaService.getApiStatus(COMPANY_ID);
                    assertNotNull(status);
                    assertEquals("NO_CONFIGURED", status);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    fail("No debería lanzar excepción: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert
        assertTrue(completed, "Todos los hilos deberían completar sin timeout");
        assertEquals(threadCount, successCount.get(), "Todos los hilos deberían ejecutar exitosamente");
    }

    @Test
    @DisplayName("isAvailable debe ser thread-safe con múltiples llamadas concurrentes")
    void isAvailable_shouldBeThreadSafe() throws InterruptedException {
        // Arrange
        when(companyApiKeyService.getDecryptedKey(COMPANY_ID, CompanyApiKey.Provider.KEEPA))
                .thenReturn(Optional.empty());
        keepaService.init();

        int threadCount = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger trueCount = new AtomicInteger(0);
        AtomicInteger falseCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(20);

        // Act
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    if (keepaService.isAvailable(COMPANY_ID)) {
                        trueCount.incrementAndGet();
                    } else {
                        falseCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    fail("No debería lanzar excepción: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert
        assertTrue(completed, "Todos los hilos deberían completar sin timeout");
        assertEquals(threadCount, trueCount.get() + falseCount.get());
        assertEquals(threadCount, falseCount.get());
    }
}
