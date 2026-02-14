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

        @NotBlank(message = "El código de empresa es obligatorio")
        @Size(min = 8, max = 8, message = "El código de empresa debe tener 8 caracteres")
        private String companyCode;
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
        private Long companyId;
        private String companyName;

        public static AuthResponse of(String token, Long userId, String username, String email, 
                                       String role, Long companyId, String companyName) {
            return AuthResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .userId(userId)
                    .username(username)
                    .email(email)
                    .role(role)
                    .companyId(companyId)
                    .companyName(companyName)
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
        private Long companyId;
        private String companyName;
        private String companyType;
        private String companyPlan;
        private String role;
        private Long totalProducts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChangePasswordRequest {
        @NotBlank(message = "La contraseña actual es obligatoria")
        private String currentPassword;

        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "La contraseña debe contener al menos una mayúscula, una minúscula y un número"
        )
        private String newPassword;
    }

    /**
     * DTO para que un COMPANY_ADMIN cree un empleado en su empresa.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateEmployeeRequest {
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
        
        // Optional: Only used by ADMIN. If null, uses creator's company.
        private Long companyId;
        
        // Optional: Role for the new user. Default: EMPLOYEE
        private String role;
    }

    /**
     * DTO para que el ADMIN general cree una empresa con su administrador.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateCompanyRequest {
        @NotBlank(message = "El nombre de la empresa es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        private String name;

        @Size(max = 50, message = "El tipo de negocio no puede exceder 50 caracteres")
        private String businessType;

        @Size(max = 20, message = "El CIF/NIF no puede exceder 20 caracteres")
        private String taxId;

        @NotBlank(message = "El username del administrador es obligatorio")
        @Size(min = 3, max = 50)
        private String adminUsername;

        @NotBlank(message = "El email del administrador es obligatorio")
        @Email(message = "El email debe tener un formato válido")
        private String adminEmail;

        @NotBlank(message = "La contraseña del administrador es obligatoria")
        @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "La contraseña debe contener al menos una mayúscula, una minúscula y un número"
        )
        private String adminPassword;
    }

    /**
     * Respuesta con los datos de la empresa creada, incluyendo el código auto-generado.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompanyResponse {
        private Long id;
        private String name;
        private String companyCode;
        private String businessType;
        private String taxId;
        private String plan;
        private String adminUsername;
    }
}

