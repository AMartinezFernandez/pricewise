package com.alvaro.pricewise.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de bienvenida en la raíz.
 * Evita que los navegadores ofrezcan descargar un archivo al abrir la URL base
 * (Spring Security devolvía 401 sin Content-Type).
 */
@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
                "service", "PriceWise API",
                "version", "1.0.0",
                "status", "UP",
                "timestamp", LocalDateTime.now(),
                "endpoints", Map.of(
                        "health", "/api/health",
                        "auth", "/api/auth/login"
                )
        ));
    }
}
