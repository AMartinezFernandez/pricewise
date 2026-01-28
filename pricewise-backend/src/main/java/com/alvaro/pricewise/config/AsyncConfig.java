package com.alvaro.pricewise.config;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuracion de pools de hilos para tareas asincronas.
 * 
 * Esta clase define dos pools de hilos:
 * - taskExecutor: Pool general para tareas asíncronas de la aplicación
 * - keepaExecutor: Pool específico para llamadas a la API de Keepa (rate-limited)
 * 
 * Características de seguridad multihilo implementadas:
 * - Shutdown graceful con tiempo de espera para completar tareas pendientes
 * - Manejo centralizado de excepciones no capturadas en hilos asíncronos
 * - CallerRunsPolicy para evitar pérdida de tareas cuando la cola está llena
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    // Constantes de configuración para el pool general
    private static final int TASK_CORE_POOL_SIZE = 5;
    private static final int TASK_MAX_POOL_SIZE = 10;
    private static final int TASK_QUEUE_CAPACITY = 100;
    private static final String TASK_THREAD_PREFIX = "Async-";

    // Constantes de configuración para el pool de Keepa
    private static final int KEEPA_CORE_POOL_SIZE = 2;
    private static final int KEEPA_MAX_POOL_SIZE = 5;
    private static final int KEEPA_QUEUE_CAPACITY = 50;
    private static final String KEEPA_THREAD_PREFIX = "Keepa-";

    // Tiempo de espera para shutdown graceful (en segundos)
    private static final int AWAIT_TERMINATION_SECONDS = 30;

    /**
     * Pool general para tareas asíncronas.
     * 
     * Configuración:
     * - Core: 5 hilos siempre activos
     * - Max: 10 hilos en picos de carga
     * - Cola: 100 tareas en espera
     * - Shutdown graceful: espera hasta 30s para completar tareas
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(TASK_CORE_POOL_SIZE);
        executor.setMaxPoolSize(TASK_MAX_POOL_SIZE);
        executor.setQueueCapacity(TASK_QUEUE_CAPACITY);
        executor.setThreadNamePrefix(TASK_THREAD_PREFIX);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // Shutdown graceful: espera a que las tareas terminen
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(AWAIT_TERMINATION_SECONDS);
        
        executor.initialize();
        log.info("Pool taskExecutor inicializado: core={}, max={}, queue={}", 
                TASK_CORE_POOL_SIZE, TASK_MAX_POOL_SIZE, TASK_QUEUE_CAPACITY);
        return executor;
    }

    /**
     * Pool específico para llamadas a APIs externas (Keepa).
     * 
     * Configuración limitada para respetar rate limits de la API:
     * - Core: 2 hilos (mínimo para concurrencia)
     * - Max: 5 hilos (respetando límite de 3 requests concurrentes del Semaphore)
     * - Cola: 50 tareas en espera
     * - Shutdown graceful: espera hasta 30s para completar tareas
     */
    @Bean(name = "keepaExecutor")
    public Executor keepaExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(KEEPA_CORE_POOL_SIZE);
        executor.setMaxPoolSize(KEEPA_MAX_POOL_SIZE);
        executor.setQueueCapacity(KEEPA_QUEUE_CAPACITY);
        executor.setThreadNamePrefix(KEEPA_THREAD_PREFIX);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // Shutdown graceful: espera a que las tareas terminen
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(AWAIT_TERMINATION_SECONDS);
        
        executor.initialize();
        log.info("Pool keepaExecutor inicializado: core={}, max={}, queue={}", 
                KEEPA_CORE_POOL_SIZE, KEEPA_MAX_POOL_SIZE, KEEPA_QUEUE_CAPACITY);
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return taskExecutor();
    }

    /**
     * Manejador de excepciones no capturadas en métodos @Async.
     * 
     * Captura y registra cualquier excepción que ocurra en hilos asíncronos
     * para evitar pérdida silenciosa de errores.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) -> 
            log.error("Excepcion no capturada en metodo asincrono: {}.{} - Error: {}", 
                    method.getDeclaringClass().getSimpleName(),
                    method.getName(), 
                    ex.getMessage(), 
                    ex);
    }
}
