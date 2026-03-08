package com.alvaro.pricewise.dto.admin;

import com.alvaro.pricewise.entity.User;
import jakarta.validation.constraints.Pattern;

public class AdminDTOs {

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
            @Pattern(regexp = "ADMIN|COMPANY_ADMIN|EMPLOYEE", message = "Rol debe ser ADMIN, COMPANY_ADMIN o EMPLOYEE")
            String role,
            Boolean active
    ) {}

    public record PasswordChangeRequest(String newPassword) {}

    public record RoleChangeRequest(
            @Pattern(regexp = "ADMIN|COMPANY_ADMIN|EMPLOYEE", message = "Rol debe ser ADMIN, COMPANY_ADMIN o EMPLOYEE")
            String role
    ) {}

    public record StatusChangeRequest(Boolean active) {}
}
