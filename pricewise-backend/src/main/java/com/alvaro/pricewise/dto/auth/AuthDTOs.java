package com.alvaro.pricewise.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDTOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoginRequest {
        @NotBlank(message = "El email o username es obligatorio")
        private String emailOrUsername;

        @NotBlank(message = "La contraseña es obligatoria")
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegisterRequest {
        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
        private String username;

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe tener un formato válido")
        private String email;

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "La contraseña debe contener al menos una mayúscula, una minúscula y un número"
        )
        private String password;

        @Size(max = 100, message = "El nombre del negocio no puede exceder 100 caracteres")
        private String businessName;
        
        @Size(max = 50, message = "El tipo de negocio no puede exceder 50 caracteres")
        private String businessType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuthResponse {
        private String token;
        private String type;
        private Long userId;
        private String username;
        private String email;
        private String role;

        public static AuthResponse of(String token, Long userId, String username, String email, String role) {
            return AuthResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .userId(userId)
                    .username(username)
                    .email(email)
                    .role(role)
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserProfileResponse {
        private Long id;
        private String username;
        private String email;
        private String businessName;
        private String businessType;
        private String role;
        private Long totalProducts;
    }
}
