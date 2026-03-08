package com.alvaro.pricewise.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.pricewise.dto.admin.AdminDTOs.PasswordChangeRequest;
import com.alvaro.pricewise.dto.admin.AdminDTOs.UpdateUserRequest;
import com.alvaro.pricewise.dto.admin.AdminDTOs.UserDetail;
import com.alvaro.pricewise.dto.admin.AdminDTOs.UserSummary;
import com.alvaro.pricewise.dto.admin.DashboardStatsDTO;
import com.alvaro.pricewise.dto.auth.AuthDTOs.CompanyResponse;
import com.alvaro.pricewise.dto.auth.AuthDTOs.CreateCompanyRequest;
import com.alvaro.pricewise.entity.Company;
import com.alvaro.pricewise.entity.User;
import com.alvaro.pricewise.exception.BadRequestException;
import com.alvaro.pricewise.exception.ResourceNotFoundException;
import com.alvaro.pricewise.repository.AlertRepository;
import com.alvaro.pricewise.repository.CompanyRepository;
import com.alvaro.pricewise.repository.CompetitorRepository;
import com.alvaro.pricewise.repository.ProductRepository;
import com.alvaro.pricewise.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;
    private final AlertRepository alertRepository;
    private final CompetitorRepository competitorRepository;
    private final KeepaService keepaService;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final Scheduler scheduler;

    // ─── Usuarios ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<UserSummary> getAllUsers() {
        Map<Long, Long> productCountByCompany = new java.util.HashMap<>();
        for (Object[] row : productRepository.countActiveProductsGroupedByCompany()) {
            productCountByCompany.put((Long) row[0], (Long) row[1]);
        }

        return userRepository.findAll().stream()
                .map(user -> {
                    long count = user.getCompany() != null
                            ? productCountByCompany.getOrDefault(user.getCompany().getId(), 0L)
                            : 0L;
                    return UserSummary.from(user, count);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDetail getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return UserDetail.from(user);
    }

    @Transactional
    public UserDetail updateUser(Long userId, UpdateUserRequest request) {
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
                throw new BadRequestException("Rol inválido: " + request.role());
            }
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }

        return UserDetail.from(userRepository.save(user));
    }

    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        if (request.newPassword() == null || request.newPassword().isBlank()) {
            throw new BadRequestException("La nueva contraseña es obligatoria");
        }
        if (request.newPassword().length() < 6) {
            throw new BadRequestException("La contraseña debe tener al menos 6 caracteres");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public UserSummary changeUserRole(Long userId, String role) {
        if (role == null || role.isBlank()) {
            throw new BadRequestException("El rol es obligatorio");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        try {
            user.setRole(User.Role.valueOf(role.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Rol inválido: " + role);
        }

        return UserSummary.from(userRepository.save(user));
    }

    @Transactional
    public UserSummary changeUserStatus(Long userId, Boolean active) {
        if (active == null) {
            throw new BadRequestException("El campo 'active' es obligatorio");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        user.setActive(active);
        return UserSummary.from(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Limpiar FK antes del borrado real (alertas y productos se mantienen para la empresa)
        alertRepository.nullifyUserForAlerts(userId);
        productRepository.nullifyCreatedByForUser(userId);

        userRepository.delete(user);
    }

    // ─── Empresas ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CompanyResponse> getAllCompanies() {
        return companyRepository.findAll().stream()
                .map(this::mapToCompanyResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CompanyResponse getCompany(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        return mapToCompanyResponse(company);
    }

    @Transactional
    public CompanyResponse createCompany(CreateCompanyRequest request) {
        return authService.createCompany(request);
    }

    private CompanyResponse mapToCompanyResponse(Company company) {
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

    // ─── Dashboard y estadísticas ──────────────────────────

    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByActiveTrue();
        long totalCompanies = companyRepository.count();
        long activeCompanies = companyRepository.countByActive(true);
        long totalProducts = productRepository.count();
        long trackedProducts = productRepository.countTrackedProducts();
        long competitorsTracked = competitorRepository.count();

        String schedulerStatus = getSchedulerStatus();

        Map<String, Long> productsByCategory = new LinkedHashMap<>();
        for (Object[] row : productRepository.countProductsGroupedByCategory()) {
            productsByCategory.put((String) row[0], (Long) row[1]);
        }

        Map<String, Long> userCountByCompany = new LinkedHashMap<>();
        for (Object[] row : userRepository.countUsersGroupedByCompany()) {
            userCountByCompany.put((String) row[0], (Long) row[1]);
        }

        return DashboardStatsDTO.builder()
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
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
        return Map.of(
                "totalUsers", userRepository.count(),
                "activeUsers", userRepository.countByActiveTrue(),
                "adminUsers", userRepository.countByRole(User.Role.ADMIN),
                "companyAdmins", userRepository.countByRole(User.Role.COMPANY_ADMIN),
                "employees", userRepository.countByRole(User.Role.EMPLOYEE),
                "totalCompanies", companyRepository.count()
        );
    }

    private String getSchedulerStatus() {
        try {
            if (scheduler.isShutdown()) return "SHUTDOWN";
            if (scheduler.isInStandbyMode()) return "STANDBY";
            if (scheduler.isStarted()) return "RUNNING";
        } catch (SchedulerException e) {
            return "ERROR";
        }
        return "UNKNOWN";
    }
}
