package com.alvaro.pricewise.dto.user;

import com.alvaro.pricewise.entity.User;

public class UserDTOs {

    public record UserSummaryDTO(
            Long id,
            String username,
            String email,
            String companyName,
            String role,
            Boolean active,
            Long productCount
    ) {
        public static UserSummaryDTO from(User user) {
            return from(user, 0L);
        }

        public static UserSummaryDTO from(User user, long productCount) {
            return new UserSummaryDTO(
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
}
