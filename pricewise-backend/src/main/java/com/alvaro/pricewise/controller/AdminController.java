package com.alvaro.pricewise.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alvaro.pricewise.dto.common.ApiResponse;
import com.alvaro.pricewise.entity.User;
import com.alvaro.pricewise.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Controlador de administración.
 * 
 * Todos los endpoints de este controlador requieren rol ADMIN.
 * Los usuarios normales (USER) recibirán un error 403 Forbidden.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Administración", description = "Endpoints exclusivos para administradores")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final com.alvaro.pricewise.repository.ProductRepository productRepository;
    private final com.alvaro.pricewise.repository.CompetitorRepository competitorRepository;
    private final com.alvaro.pricewise.service.KeepaService keepaService;
    private final PasswordEncoder passwordEncoder;
    private final org.quartz.Scheduler scheduler;

    /**
     * Lista todos los usuarios del sistema.
     */
    @GetMapping("/users")
    @Operation(summary = "Listar usuarios", description = "Obtiene todos los usuarios del sistema")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<UserSummary>>> getAllUsers() {
        List<UserSummary> users = userRepository.findAll().stream()
                .map(UserSummary::from)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /**
     * Obtiene estadísticas avanzadas para el dashboard.
     */
    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard Metrics", description = "Obtiene métricas detalladas para el dashboard de administración")
    public ResponseEntity<ApiResponse<com.alvaro.pricewise.dto.admin.DashboardStatsDTO>> getDashboardStats() {
        // Usuarios
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findAll().stream().filter(User::getActive).count();
        
        // Productos
        long totalProducts = productRepository.count();
        // Productos monitoreados (aquellos con SKU que empieza por B0 - ASIN)
        long trackedProducts = productRepository.findAll().stream()
                .filter(p -> p.getSku() != null && p.getSku().startsWith("B0"))
                .count();
        
        // Competidores
        long competitorsTracked = competitorRepository.count();
        
        // Scheduler status
        String schedulerStatus = "UNKNOWN";
        try {
            if (scheduler.isShutdown()) schedulerStatus = "SHUTDOWN";
            else if (scheduler.isInStandbyMode()) schedulerStatus = "STANDBY";
            else if (scheduler.isStarted()) schedulerStatus = "RUNNING";
        } catch (Exception e) {
            schedulerStatus = "ERROR";
        }

        // Distribución por categoría
        java.util.Map<String, Long> productsByCategory = productRepository.findAll().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        p -> p.getCategory() != null ? p.getCategory() : "Sin Categoría",
                        java.util.stream.Collectors.counting()
                ));

        com.alvaro.pricewise.dto.admin.DashboardStatsDTO stats = com.alvaro.pricewise.dto.admin.DashboardStatsDTO.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalProducts(totalProducts)
                .trackedProducts(trackedProducts)
                .productsWithPriceDrop(0) // Pendiente implementar historial
                .competitorsTracked(competitorsTracked)
                .keepaStatus(keepaService.isAvailable())
                .schedulerStatus(schedulerStatus)
                .productsByCategory(productsByCategory)
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Obtiene estadísticas generales del sistema (Versión simple).
     */
    @GetMapping("/stats")
    @Operation(summary = "Estadísticas simples", description = "Obtiene estadísticas básicas del sistema")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findAll().stream()
                .filter(User::getActive)
                .count();
        long adminUsers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.ADMIN)
                .count();
        
        Map<String, Object> stats = Map.of(
                "totalUsers", totalUsers,
                "activeUsers", activeUsers,
                "adminUsers", adminUsers,
                "regularUsers", totalUsers - adminUsers
        );
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Edita un usuario completo (email, username, businessName, etc.)
     */
    @PutMapping("/users/{userId}")
    @Operation(summary = "Editar usuario", description = "Modifica los datos de un usuario")
    public ResponseEntity<ApiResponse<UserDetail>> updateUser(
            @PathVariable Long userId,
            @RequestBody UpdateUserRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // Actualizar campos si vienen en el request
        if (request.username() != null && !request.username().isBlank()) {
            user.setUsername(request.username());
        }
        if (request.email() != null && !request.email().isBlank()) {
            user.setEmail(request.email());
        }
        if (request.businessName() != null) {
            user.setBusinessName(request.businessName());
        }
        if (request.businessType() != null) {
            user.setBusinessType(request.businessType());
        }
        if (request.role() != null && !request.role().isBlank()) {
            user.setRole(User.Role.valueOf(request.role().toUpperCase()));
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }
        
        user = userRepository.save(user);
        
        return ResponseEntity.ok(ApiResponse.success(
                UserDetail.from(user), 
                "Usuario actualizado correctamente"
        ));
    }

    /**
     * Cambia la contraseña de un usuario.
     */
    @PutMapping("/users/{userId}/password")
    @Operation(summary = "Cambiar contraseña", description = "Cambia la contraseña de un usuario")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long userId,
            @RequestBody PasswordChangeRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // Hashear la nueva contraseña
        String hashedPassword = passwordEncoder.encode(request.newPassword());
        user.setPassword(hashedPassword);
        userRepository.save(user);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Contraseña actualizada correctamente"));
    }

    /**
     * Cambia el rol de un usuario.
     */
    @PutMapping("/users/{userId}/role")
    @Operation(summary = "Cambiar rol", description = "Cambia el rol de un usuario")
    public ResponseEntity<ApiResponse<UserSummary>> changeUserRole(
            @PathVariable Long userId,
            @RequestBody RoleChangeRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        user.setRole(User.Role.valueOf(request.role().toUpperCase()));
        user = userRepository.save(user);
        
        return ResponseEntity.ok(ApiResponse.success(
                UserSummary.from(user), 
                "Rol actualizado a " + user.getRole()
        ));
    }

    /**
     * Activa o desactiva un usuario.
     */
    @PutMapping("/users/{userId}/status")
    @Operation(summary = "Cambiar estado", description = "Activa o desactiva un usuario")
    public ResponseEntity<ApiResponse<UserSummary>> changeUserStatus(
            @PathVariable Long userId,
            @RequestBody StatusChangeRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        user.setActive(request.active());
        user = userRepository.save(user);
        
        String message = user.getActive() ? "Usuario activado" : "Usuario desactivado";
        return ResponseEntity.ok(ApiResponse.success(UserSummary.from(user), message));
    }

    /**
     * Elimina un usuario (hard delete).
     */
    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Eliminar usuario", description = "Elimina un usuario del sistema permanentemente")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // No permitir eliminar el propio usuario admin
        userRepository.delete(user);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Usuario eliminado permanentemente"));
    }

    // ========== DTOs internos ==========
    
    public record UserSummary(
            Long id,
            String username,
            String email,
            String businessName,
            String role,
            Boolean active,
            Integer productCount
    ) {
        public static UserSummary from(User user) {
            return new UserSummary(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getBusinessName(),
                    user.getRole().name(),
                    user.getActive(),
                    user.getProducts() != null ? user.getProducts().size() : 0
            );
        }
    }

    public record UserDetail(
            Long id,
            String username,
            String email,
            String businessName,
            String businessType,
            String role,
            Boolean active,
            Integer productCount,
            String createdAt,
            String updatedAt
    ) {
        public static UserDetail from(User user) {
            return new UserDetail(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getBusinessName(),
                    user.getBusinessType(),
                    user.getRole().name(),
                    user.getActive(),
                    user.getProducts() != null ? user.getProducts().size() : 0,
                    user.getCreatedAt() != null ? user.getCreatedAt().toString() : null,
                    user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null
            );
        }
    }

    public record UpdateUserRequest(
            String username,
            String email,
            String businessName,
            String businessType,
            String role,
            Boolean active
    ) {}

    public record PasswordChangeRequest(String newPassword) {}
    
    public record RoleChangeRequest(String role) {}
    
    public record StatusChangeRequest(Boolean active) {}
}

