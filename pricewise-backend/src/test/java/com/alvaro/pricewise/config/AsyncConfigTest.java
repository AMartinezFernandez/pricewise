package com.alvaro.pricewise.config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;

/**
 * Tests para AsyncConfig verificando la configuración de los pools de hilos.
 */
@SpringBootTest
@ActiveProfiles("dev")
class AsyncConfigTest {

    @Autowired
    @Qualifier("taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    @Qualifier("keepaExecutor")
    private ThreadPoolTaskExecutor keepaExecutor;

    @Test
    @DisplayName("taskExecutor debe tener la configuración correcta")
    void taskExecutor_shouldHaveCorrectConfiguration() {
        assertNotNull(taskExecutor);
        assertEquals(5, taskExecutor.getCorePoolSize());
        assertEquals(10, taskExecutor.getMaxPoolSize());
        assertTrue(taskExecutor.getThreadNamePrefix().startsWith("Async"));
    }

    @Test
    @DisplayName("keepaExecutor debe tener la configuración correcta")
    void keepaExecutor_shouldHaveCorrectConfiguration() {
        assertNotNull(keepaExecutor);
        assertEquals(2, keepaExecutor.getCorePoolSize());
        assertEquals(5, keepaExecutor.getMaxPoolSize());
        assertTrue(keepaExecutor.getThreadNamePrefix().startsWith("Keepa"));
    }

    @Test
    @DisplayName("taskExecutor debe ejecutar tareas concurrentemente")
    void taskExecutor_shouldExecuteTasksConcurrently() throws InterruptedException {
        // Arrange
        int taskCount = 20;
        CountDownLatch latch = new CountDownLatch(taskCount);
        AtomicInteger completedCount = new AtomicInteger(0);

        // Act
        for (int i = 0; i < taskCount; i++) {
            taskExecutor.execute(() -> {
                try {
                    Thread.sleep(50); // Simular trabajo
                    completedCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Assert
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "Todas las tareas deberían completar");
        assertEquals(taskCount, completedCount.get());
    }

    @Test
    @DisplayName("keepaExecutor debe respetar límite de hilos")
    void keepaExecutor_shouldRespectThreadLimit() throws InterruptedException {
        // Arrange
        int taskCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(taskCount);
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        AtomicInteger currentConcurrent = new AtomicInteger(0);

        // Act
        for (int i = 0; i < taskCount; i++) {
            keepaExecutor.execute(() -> {
                try {
                    int current = currentConcurrent.incrementAndGet();
                    maxConcurrent.updateAndGet(max -> Math.max(max, current));
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    currentConcurrent.decrementAndGet();
                    endLatch.countDown();
                }
            });
        }

        // Assert
        boolean completed = endLatch.await(15, TimeUnit.SECONDS);
        assertTrue(completed, "Todas las tareas deberían completar");
        assertTrue(maxConcurrent.get() <= keepaExecutor.getMaxPoolSize(), 
                "No debería exceder el máximo de hilos: " + maxConcurrent.get());
    }
}
