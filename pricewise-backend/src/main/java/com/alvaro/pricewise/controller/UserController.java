package com.alvaro.pricewise.controller;

import com.alvaro.pricewise.dto.common.ApiResponse;
import com.alvaro.pricewise.entity.User;
import com.alvaro.pricewise.repository.UserRepository;
import com.alvaro.pricewise.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Users", description = "Gestion de usuarios por empresa")
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Listar usuarios", description = "ADMIN ve todos, COMPANY_ADMIN ve solo su empresa")
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
    @Operation(summary = "Contar usuarios", description = "ADMIN cuenta todos, COMPANY_ADMIN cuenta su empresa")
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
