package com.alvaro.pricewise.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alvaro.pricewise.dto.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller para administración del Scheduler de monitoreo de precios.
 * Solo accesible para administradores.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/scheduler")
@RequiredArgsConstructor
@Tag(name = "Admin - Scheduler", description = "Control del scheduler de monitoreo de precios")
public class SchedulerController {

    private final Scheduler scheduler;

    /**
     * Obtiene el estado actual del scheduler
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Estado del scheduler", description = "Obtiene información del estado actual del scheduler")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSchedulerStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("running", scheduler.isStarted() && !scheduler.isInStandbyMode());
            status.put("standby", scheduler.isInStandbyMode());
            status.put("shutdown", scheduler.isShutdown());
            status.put("schedulerName", scheduler.getSchedulerName());
            status.put("timestamp", LocalDateTime.now());

            // Info del Job de monitoreo de precios
            JobKey jobKey = JobKey.jobKey("priceMonitorJob");
            if (scheduler.checkExists(jobKey)) {
                JobDetail jobDetail = scheduler.getJobDetail(jobKey);
                status.put("priceMonitorJobExists", true);
                status.put("priceMonitorJobDescription", jobDetail.getDescription());
                
                // Próxima ejecución
                Trigger trigger = scheduler.getTriggersOfJob(jobKey).stream().findFirst().orElse(null);
                if (trigger != null) {
                    status.put("nextFireTime", trigger.getNextFireTime());
                    status.put("previousFireTime", trigger.getPreviousFireTime());
                }
            } else {
                status.put("priceMonitorJobExists", false);
            }

            return ResponseEntity.ok(ApiResponse.success(status));
        } catch (SchedulerException e) {
            log.error("Error obteniendo estado del scheduler: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    /**
     * Ejecuta el Job de monitoreo de precios inmediatamente
     */
    @PostMapping("/trigger-now")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ejecutar ahora", description = "Dispara el Job de monitoreo de precios inmediatamente")
    public ResponseEntity<ApiResponse<String>> triggerPriceMonitorNow() {
        try {
            JobKey jobKey = JobKey.jobKey("priceMonitorJob");
            
            if (!scheduler.checkExists(jobKey)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Job de monitoreo no encontrado"));
            }

            scheduler.triggerJob(jobKey);
            log.info("🚀 Job de monitoreo de precios disparado manualmente");
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Job de monitoreo ejecutándose", 
                    "El Job se ha iniciado. Revisa los logs para ver el progreso."));
        } catch (SchedulerException e) {
            log.error("Error disparando Job: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    /**
     * Pausa el scheduler
     */
    @PostMapping("/pause")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Pausar scheduler", description = "Pone el scheduler en modo standby")
    public ResponseEntity<ApiResponse<String>> pauseScheduler() {
        try {
            scheduler.standby();
            log.info("⏸️ Scheduler pausado");
            return ResponseEntity.ok(ApiResponse.success("Scheduler pausado", "OK"));
        } catch (SchedulerException e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    /**
     * Reanuda el scheduler
     */
    @PostMapping("/resume")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reanudar scheduler", description = "Reanuda el scheduler después de una pausa")
    public ResponseEntity<ApiResponse<String>> resumeScheduler() {
        try {
            scheduler.start();
            log.info("▶️ Scheduler reanudado");
            return ResponseEntity.ok(ApiResponse.success("Scheduler reanudado", "OK"));
        } catch (SchedulerException e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }
}
