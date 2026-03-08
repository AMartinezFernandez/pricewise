package com.alvaro.pricewise.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alvaro.pricewise.dto.apikey.ApiKeyDTOs.ApiKeyResponse;
import com.alvaro.pricewise.dto.apikey.ApiKeyDTOs.SaveApiKeyRequest;
import com.alvaro.pricewise.dto.common.ApiResponse;
import com.alvaro.pricewise.security.UserPrincipal;
import com.alvaro.pricewise.service.CompanyApiKeyService;
import com.alvaro.pricewise.service.KeepaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/api-keys")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'ADMIN')")
public class ApiKeyController {

    private final CompanyApiKeyService apiKeyService;
    private final KeepaService keepaService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ApiKeyResponse>>> getApiKeys(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<ApiKeyResponse> keys = apiKeyService.getApiKeys(userPrincipal.requireCompanyId());
        return ResponseEntity.ok(ApiResponse.success(keys));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ApiKeyResponse>> saveApiKey(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody SaveApiKeyRequest request) {
        Long companyId = userPrincipal.requireCompanyId();
        ApiKeyResponse response = apiKeyService.saveApiKey(companyId, request);
        keepaService.invalidateCache(companyId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "API key guardada correctamente"));
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> toggleApiKey(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        Long companyId = userPrincipal.requireCompanyId();
        ApiKeyResponse response = apiKeyService.toggleApiKey(companyId, id);
        keepaService.invalidateCache(companyId);
        String message = response.getEnabled() ? "API key habilitada" : "API key deshabilitada";
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteApiKey(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        Long companyId = userPrincipal.requireCompanyId();
        apiKeyService.deleteApiKey(companyId, id);
        keepaService.invalidateCache(companyId);
        return ResponseEntity.ok(ApiResponse.success(null, "API key eliminada"));
    }
}
