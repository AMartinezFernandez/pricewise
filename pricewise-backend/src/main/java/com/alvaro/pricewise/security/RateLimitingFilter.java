package com.alvaro.pricewise.security;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Rate limiting para endpoints de autenticación.
 * Limita intentos por IP para prevenir ataques de fuerza bruta.
 * Incluye evicción periódica de entradas expiradas para evitar memory leaks.
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_SECONDS = 60;
    private static final int MAX_MAP_SIZE = 10_000;
    private static final long CLEANUP_INTERVAL_SECONDS = 300; // Limpiar cada 5 minutos

    private final ConcurrentHashMap<String, RateInfo> requests = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanup = new AtomicLong(Instant.now().getEpochSecond());

    @Override
    protected void doFilterInternal(@org.springframework.lang.NonNull HttpServletRequest request, @org.springframework.lang.NonNull HttpServletResponse response,
            @org.springframework.lang.NonNull FilterChain filterChain) throws ServletException, IOException {

        cleanupIfNeeded();

        String clientIp = getClientIp(request);
        RateInfo rateInfo = requests.compute(clientIp, (key, existing) -> {
            long now = Instant.now().getEpochSecond();
            if (existing == null || now - existing.windowStart >= WINDOW_SECONDS) {
                return new RateInfo(now, 1);
            }
            existing.count++;
            return existing;
        });

        if (rateInfo.count > MAX_ATTEMPTS) {
            log.warn("Rate limit excedido para IP: {}", clientIp);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Demasiados intentos. Espera un momento antes de reintentar.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@org.springframework.lang.NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/auth/login") && !path.startsWith("/api/auth/register");
    }

    /**
     * Usa remoteAddr directamente. X-Forwarded-For solo es fiable detrás de un
     * proxy de confianza configurado, y en el MVP no hay proxy inverso.
     */
    private String getClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    /**
     * Limpia entradas expiradas periódicamente para evitar crecimiento ilimitado del mapa.
     */
    private void cleanupIfNeeded() {
        long now = Instant.now().getEpochSecond();
        long last = lastCleanup.get();

        // Limpiar por intervalo de tiempo o si el mapa crece demasiado
        if ((now - last >= CLEANUP_INTERVAL_SECONDS || requests.size() > MAX_MAP_SIZE)
                && lastCleanup.compareAndSet(last, now)) {
            int sizeBefore = requests.size();
            requests.entrySet().removeIf(entry ->
                    now - entry.getValue().windowStart >= WINDOW_SECONDS);
            int removed = sizeBefore - requests.size();
            if (removed > 0) {
                log.debug("Rate limiter cleanup: {} entradas expiradas eliminadas", removed);
            }
        }
    }

    private static class RateInfo {
        long windowStart;
        int count;

        RateInfo(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
