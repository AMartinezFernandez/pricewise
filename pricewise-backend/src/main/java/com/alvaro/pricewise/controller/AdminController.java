package com.alvaro.pricewise.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alvaro.pricewise.dto.auth.AuthDTOs.CompanyResponse;
import com.alvaro.pricewise.dto.auth.AuthDTOs.CreateCompanyRequest;
import com.alvaro.pricewise.dto.common.ApiResponse;
import com.alvaro.pricewise.entity.User;
import com.alvaro.pricewise.exception.ResourceNotFoundException;
import com.alvaro.pricewise.repository.ProductRepository;
import com.alvaro.pricewise.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controlador de administración.
 * 
 * Todos los endpoints de este controlador requieren rol ADMIN.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Administración", description = "Endpoints exclusivos para administradores")
@PreAuthorize("hasRole('ADMIN')")
@SuppressWarnings("null")
public class AdminController {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final com.alvaro.pricewise.repository.CompanyRepository companyRepository;
    private final com.alvaro.pricewise.repository.CompetitorRepository competitorRepository;
    private final com.alvaro.pricewise.service.KeepaService keepaService;
    private final com.alvaro.pricewise.service.AuthService authService;
    private final com.alvaro.pricewise.service.AuditService auditService;
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
                .map(user -> {
                    long count = user.getCompany() != null
                            ? productRepository.countByCompanyIdAndActiveTrue(user.getCompany().getId())
                            : 0L;
                    return UserSummary.from(user, count);
                })
                .toList();

        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /**
     * Obtiene los detalles de un usuario específico.
     */
    @GetMapping("/users/{userId}")
    @Operation(summary = "Detalle de usuario", description = "Obtiene los detalles completos de un usuario")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<UserDetail>> getUser(@PathVariable @org.springframework.lang.NonNull Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        return ResponseEntity.ok(ApiResponse.success(UserDetail.from(user)));
    }

    /**
     * Lista todas las empresas.
     */
    @GetMapping("/companies")
    @Operation(summary = "Listar empresas", description = "Obtiene todas las empresas del sistema")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<CompanyResponse>>> getAllCompanies() {
        List<CompanyResponse> companies = companyRepository.findAll().stream()
                .map(this::mapToCompanyResponse)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(companies));
    }

    /**
     * Obtiene los detalles de una empresa específica.
     */
    @GetMapping("/companies/{companyId}")
    @Operation(summary = "Detalle de empresa", description = "Obtiene los detalles de una empresa")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<CompanyResponse>> getCompany(@PathVariable @org.springframework.lang.NonNull Long companyId) {
        com.alvaro.pricewise.entity.Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        
        return ResponseEntity.ok(ApiResponse.success(mapToCompanyResponse(company)));
    }
    
    private CompanyResponse mapToCompanyResponse(com.alvaro.pricewise.entity.Company company) {
        // Buscar el administrador de la empresa (puede haber varios, tomamos el primero o indicamos 'N/A')
        String adminUsername = company.getUsers().stream()
                .filter(u -> u.getRole() == User.Role.COMPANY_ADMIN)
                .map(User::getUsername)
                .findFirst()
                .orElse("N/A");

        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .companyCode(company.getCompanyCode())
                .businessType(company.getBusinessType())
                .taxId(company.getTaxId())
                .plan(company.getPlan() != null ? company.getPlan().name() : "FREE")
                .adminUsername(adminUsername)
                .build();
    }

    /**
     * Crea una empresa con su COMPANY_ADMIN.
     * Devuelve los datos de la empresa incluyendo el código auto-generado para el registro de empleados.
     */
    @PostMapping("/companies")
    @Operation(summary = "Crear empresa", 
               description = "Crea una nueva empresa con su administrador. Devuelve el código de empresa para registro de empleados.")
    public ResponseEntity<ApiResponse<CompanyResponse>> createCompany(
            @AuthenticationPrincipal com.alvaro.pricewise.security.UserPrincipal userPrincipal,
            @Valid @RequestBody CreateCompanyRequest request) {
        CompanyResponse response = authService.createCompany(request);
        auditService.logAction(userPrincipal, "CREATE_COMPANY", "COMPANY", response.getId(),
                "Empresa creada: " + request.getName());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Empresa creada exitosamente"));
    }

    /**
     * Obtiene estadísticas avanzadas para el dashboard.
     */
    /**
     * Obtiene estadísticas avanzadas para el dashboard.
     */
    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard Metrics", description = "Obtiene métricas detalladas para el dashboard de administración")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<com.alvaro.pricewise.dto.admin.DashboardStatsDTO>> getDashboardStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findAll().stream().filter(User::getActive).count();
        
        long totalCompanies = companyRepository.count();
        long activeCompanies = companyRepository.countByActive(true);
        
        long totalProducts = productRepository.count();
        long trackedProducts = productRepository.findAll().stream()
                .filter(p -> p.getAsin() != null && !p.getAsin().isBlank())
                .count();
        
        long competitorsTracked = competitorRepository.count();
        
        String schedulerStatus = "UNKNOWN";
        try {
            if (scheduler.isShutdown()) schedulerStatus = "SHUTDOWN";
            else if (scheduler.isInStandbyMode()) schedulerStatus = "STANDBY";
            else if (scheduler.isStarted()) schedulerStatus = "RUNNING";
        } catch (org.quartz.SchedulerException e) {
            schedulerStatus = "ERROR";
        }

        java.util.Map<String, Long> productsByCategory = productRepository.findAll().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        p -> p.getCategory() != null ? p.getCategory() : "Sin Categoría",
                        java.util.stream.Collectors.counting()
                ));

        java.util.Map<String, Long> userCountByCompany = userRepository.findAll().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        u -> u.getCompany() != null ? u.getCompany().getName() : "Sin Empresa",
                        java.util.stream.Collectors.counting()
                ));

        com.alvaro.pricewise.dto.admin.DashboardStatsDTO stats = com.alvaro.pricewise.dto.admin.DashboardStatsDTO.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalCompanies(totalCompanies)
                .activeCompanies(activeCompanies)
                .totalProducts(totalProducts)
                .trackedProducts(trackedProducts)
                .productsWithPriceDrop(0)
                .competitorsTracked(competitorsTracked)
                .keepaStatus(keepaService.isAvailable())
                .schedulerStatus(schedulerStatus)
                .productsByCategory(productsByCategory)
                .userCountByCompany(userCountByCompany)
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Obtiene estadísticas generales del sistema (Versión simple).
     */
    @GetMapping("/stats")
    @Operation(summary = "Estadísticas simples", description = "Obtiene estadísticas básicas del sistema")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        List<User> allUsers = userRepository.findAll();
        
        long totalUsers = allUsers.size();
        long activeUsers = allUsers.stream().filter(User::getActive).count();
        
        long adminUsers = allUsers.stream().filter(u -> u.getRole() == User.Role.ADMIN).count();
        long companyAdmins = allUsers.stream().filter(u -> u.getRole() == User.Role.COMPANY_ADMIN).count();
        long employees = allUsers.stream().filter(u -> u.getRole() == User.Role.EMPLOYEE).count();
        
        long totalCompanies = companyRepository.count();
        
        Map<String, Object> stats = Map.of(
                "totalUsers", totalUsers,
                "activeUsers", activeUsers,
                "adminUsers", adminUsers,
                "companyAdmins", companyAdmins,
                "employees", employees,
                "totalCompanies", totalCompanies
        );
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Edita un usuario (admin puede cambiar rol, estado, etc.).
     * Business name/type ahora se gestionan desde la Company.
     */
    @PutMapping("/users/{userId}")
    @Operation(summary = "Editar usuario", description = "Modifica los datos de un usuario")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ApiResponse<UserDetail>> updateUser(
            @PathVariable @org.springframework.lang.NonNull Long userId,
            @RequestBody UpdateUserRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        if (request.username() != null && !request.username().isBlank()) {
            user.setUsername(request.username());
        }
        if (request.email() != null && !request.email().isBlank()) {
            user.setEmail(request.email());
        }
        if (request.role() != null && !request.role().isBlank()) {
            try {
                user.setRole(User.Role.valueOf(request.role().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new com.alvaro.pricewise.exception.BadRequestException("Rol inválido: " + request.role());
            }
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
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable @org.springframework.lang.NonNull Long userId,
            @RequestBody PasswordChangeRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
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
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ApiResponse<UserSummary>> changeUserRole(
            @PathVariable @org.springframework.lang.NonNull Long userId,
            @RequestBody RoleChangeRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        try {
            user.setRole(User.Role.valueOf(request.role().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new com.alvaro.pricewise.exception.BadRequestException("Rol inválido: " + request.role());
        }
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
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ApiResponse<UserSummary>> changeUserStatus(
            @PathVariable @org.springframework.lang.NonNull Long userId,
            @RequestBody StatusChangeRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
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
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @AuthenticationPrincipal com.alvaro.pricewise.security.UserPrincipal userPrincipal,
            @PathVariable @org.springframework.lang.NonNull Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        auditService.logAction(userPrincipal, "DELETE_USER", "USER", userId,
                "Usuario eliminado: " + user.getUsername());
        userRepository.delete(user);

        return ResponseEntity.ok(ApiResponse.success(null, "Usuario eliminado permanentemente"));
    }

    /**
     * Consulta el log de auditoria.
     */
    @GetMapping("/audit-logs")
    @Operation(summary = "Consultar audit log", description = "Lista las operaciones registradas en el audit log")
    public ResponseEntity<ApiResponse<com.alvaro.pricewise.dto.common.PageResponse<com.alvaro.pricewise.entity.AuditLog>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<com.alvaro.pricewise.entity.AuditLog> logs = auditService.getAuditLogs(pageable);

        return ResponseEntity.ok(ApiResponse.success(
                com.alvaro.pricewise.dto.common.PageResponse.from(logs)));
    }

    // ========== DTOs internos ==========
    
    public record UserSummary(
            Long id,
            String username,
            String email,
            String companyName,
            String role,
            Boolean active,
            Long productCount
    ) {
        public static UserSummary from(User user) {
            return from(user, 0L);
        }

        public static UserSummary from(User user, long productCount) {
            return new UserSummary(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getCompany() != null ? user.getCompany().getName() : null,
                    user.getRole().name(),
                    user.getActive(),
                    productCount
            );
        }
    }

    public record UserDetail(
            Long id,
            String username,
            String email,
            String companyName,
            String companyType,
            String role,
            Boolean active,
            String createdAt,
            String updatedAt
    ) {
        public static UserDetail from(User user) {
            return new UserDetail(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getCompany() != null ? user.getCompany().getName() : null,
                    user.getCompany() != null ? user.getCompany().getBusinessType() : null,
                    user.getRole().name(),
                    user.getActive(),
                    user.getCreatedAt() != null ? user.getCreatedAt().toString() : null,
                    user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null
            );
        }
    }

    public record UpdateUserRequest(
            String username,
            String email,
            String role,
            Boolean active
    ) {}

    public record PasswordChangeRequest(String newPassword) {}
    
    public record RoleChangeRequest(String role) {}
    
    public record StatusChangeRequest(Boolean active) {}
}
