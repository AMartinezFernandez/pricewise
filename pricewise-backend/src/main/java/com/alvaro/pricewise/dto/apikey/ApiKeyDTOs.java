package com.alvaro.pricewise.dto.apikey;

import com.alvaro.pricewise.entity.CompanyApiKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class ApiKeyDTOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SaveApiKeyRequest {
        @NotNull(message = "El proveedor es obligatorio")
        @Pattern(regexp = "KEEPA", message = "Proveedor no soportado")
        private String provider;

        @NotBlank(message = "La API key es obligatoria")
        private String apiKey;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApiKeyResponse {
        private Long id;
        private String provider;
        private String maskedKey;
        private Boolean enabled;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static ApiKeyResponse fromEntity(CompanyApiKey entity, String maskedKey) {
            return ApiKeyResponse.builder()
                    .id(entity.getId())
                    .provider(entity.getProvider().name())
                    .maskedKey(maskedKey)
                    .enabled(entity.getEnabled())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .build();
        }
    }
}
