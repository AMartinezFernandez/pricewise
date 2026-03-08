package com.alvaro.pricewise.controller;

import com.alvaro.pricewise.dto.common.ApiResponse;
import com.alvaro.pricewise.entity.User;
import com.alvaro.pricewise.exception.ResourceNotFoundException;
import com.alvaro.pricewise.repository.AlertRepository;
import com.alvaro.pricewise.repository.ProductRepository;
import com.alvaro.pricewise.repository.UserRepository;
import com.alvaro.pricewise.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN')")
public class UserController {

    private final UserRepository userRepository;
    private final AlertRepository alertRepository;
    private final ProductRepository productRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<UserSummaryDTO>>> getUsers(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<User> users;
        if ("ADMIN".equals(principal.getRole())) {
            users = userRepository.findAll();
        } else {
            users = userRepository.findByCompanyId(principal.getCompanyId());
        }

        List<UserSummaryDTO> result = users.stream()
                .map(UserSummaryDTO::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/count")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Long>> getUserCount(
            @AuthenticationPrincipal UserPrincipal principal) {

        long count;
        if ("ADMIN".equals(principal.getRole())) {
            count = userRepository.count();
        } else {
            count = userRepository.countByCompanyId(principal.getCompanyId());
        }

        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @DeleteMapping("/{userId}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // COMPANY_ADMIN solo puede borrar usuarios de su empresa
        if (!"ADMIN".equals(principal.getRole())) {
            if (user.getCompany() == null || !user.getCompany().getId().equals(principal.getCompanyId())) {
                throw new ResourceNotFoundException("Usuario no encontrado");
            }
            // No puede borrarse a sí mismo
            if (user.getId().equals(principal.getUserId())) {
                throw new com.alvaro.pricewise.exception.BadRequestException("No puedes eliminarte a ti mismo");
            }
            // COMPANY_ADMIN solo puede eliminar EMPLOYEE, no admins
            if (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.COMPANY_ADMIN) {
                throw new com.alvaro.pricewise.exception.BadRequestException("No puedes eliminar a un administrador");
            }
        }

        alertRepository.nullifyUserForAlerts(userId);
        productRepository.nullifyCreatedByForUser(userId);
        userRepository.delete(user);

        return ResponseEntity.ok(ApiResponse.success(null, "Usuario eliminado"));
    }

    // DTO interno
    public record UserSummaryDTO(
            Long id,
            String username,
            String email,
            String companyName,
            String role,
            Boolean active
    ) {
        public static UserSummaryDTO from(User user) {
            return new UserSummaryDTO(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getCompany() != null ? user.getCompany().getName() : null,
                    user.getRole().name(),
                    user.getActive()
            );
        }
    }
}
