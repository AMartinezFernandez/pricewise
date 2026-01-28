package com.alvaro.pricewise.security;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Filtro de autenticación JWT.
 * 
 * Se ejecuta una vez por cada request para:
 * 1. Extraer el token JWT del header Authorization
 * 2. Validar el token (firma, expiración)
 * 3. Cargar el usuario y establecer la autenticación en el contexto de Spring Security
 * 
 * SEGURIDAD:
 * - El token se espera en formato "Bearer {token}"
 * - Si el token es inválido, la request continúa sin autenticación
 * - Las rutas protegidas rechazarán requests sin autenticación válida
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        // Extraer header de autorización
        final String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        
        // Si no hay header o no comienza con "Bearer ", continuar sin autenticación
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extraer token (quitar "Bearer ")
            final String jwt = authHeader.substring(BEARER_PREFIX.length());
            final String userEmail = jwtService.extractUsername(jwt);

            // Solo autenticar si hay email y no hay autenticación previa
            if (StringUtils.hasText(userEmail) && 
                SecurityContextHolder.getContext().getAuthentication() == null) {
                
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                
                // Validar token contra el usuario
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,  // No guardamos credenciales después de autenticar
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Establecer autenticación en el contexto de seguridad
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.debug("Usuario autenticado via JWT: {}", userEmail);
                }
            }
        } catch (Exception e) {
            // Log del error pero continuar - las rutas protegidas rechazarán la request
            log.warn("Error procesando token JWT: {}", e.getMessage());
            // No lanzamos excepción para permitir acceso a rutas públicas
        }

        filterChain.doFilter(request, response);
    }
}

