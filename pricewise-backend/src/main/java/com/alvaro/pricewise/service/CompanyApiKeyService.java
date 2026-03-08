package com.alvaro.pricewise.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.pricewise.dto.apikey.ApiKeyDTOs.ApiKeyResponse;
import com.alvaro.pricewise.dto.apikey.ApiKeyDTOs.SaveApiKeyRequest;
import com.alvaro.pricewise.entity.Company;
import com.alvaro.pricewise.entity.CompanyApiKey;
import com.alvaro.pricewise.exception.BadRequestException;
import com.alvaro.pricewise.exception.ResourceNotFoundException;
import com.alvaro.pricewise.repository.CompanyApiKeyRepository;
import com.alvaro.pricewise.repository.CompanyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyApiKeyService {

    private final CompanyApiKeyRepository apiKeyRepository;
    private final CompanyRepository companyRepository;
    private final ApiKeyEncryptionService encryptionService;

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> getApiKeys(Long companyId) {
        return apiKeyRepository.findByCompanyIdOrderByProviderAsc(companyId).stream()
                .map(key -> {
                    String decrypted = encryptionService.decrypt(key.getEncryptedKey());
                    return ApiKeyResponse.fromEntity(key, encryptionService.mask(decrypted));
                })
                .toList();
    }

    @Transactional
    public ApiKeyResponse saveApiKey(Long companyId, SaveApiKeyRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        CompanyApiKey.Provider provider;
        try {
            provider = CompanyApiKey.Provider.valueOf(request.getProvider());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Proveedor no soportado: " + request.getProvider());
        }

        String encrypted = encryptionService.encrypt(request.getApiKey());

        CompanyApiKey apiKey = apiKeyRepository.findByCompanyIdAndProvider(companyId, provider)
                .map(existing -> {
                    existing.setEncryptedKey(encrypted);
                    existing.setEnabled(true);
                    return existing;
                })
                .orElseGet(() -> CompanyApiKey.builder()
                        .company(company)
                        .provider(provider)
                        .encryptedKey(encrypted)
                        .enabled(true)
                        .build());

        apiKey = apiKeyRepository.save(apiKey);
        log.info("API key {} guardada para empresa {}", provider, companyId);

        return ApiKeyResponse.fromEntity(apiKey, encryptionService.mask(request.getApiKey()));
    }

    @Transactional
    public ApiKeyResponse toggleApiKey(Long companyId, Long keyId) {
        CompanyApiKey apiKey = apiKeyRepository.findById(keyId)
                .filter(k -> k.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new ResourceNotFoundException("API key no encontrada"));

        apiKey.setEnabled(!apiKey.getEnabled());
        apiKey = apiKeyRepository.save(apiKey);

        String decrypted = encryptionService.decrypt(apiKey.getEncryptedKey());
        log.info("API key {} {} para empresa {}", apiKey.getProvider(),
                apiKey.getEnabled() ? "habilitada" : "deshabilitada", companyId);

        return ApiKeyResponse.fromEntity(apiKey, encryptionService.mask(decrypted));
    }

    @Transactional
    public void deleteApiKey(Long companyId, Long keyId) {
        CompanyApiKey apiKey = apiKeyRepository.findById(keyId)
                .filter(k -> k.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new ResourceNotFoundException("API key no encontrada"));

        apiKeyRepository.delete(apiKey);
        log.info("API key {} eliminada para empresa {}", apiKey.getProvider(), companyId);
    }

    /**
     * Obtiene la API key descifrada de un proveedor para una empresa.
     * Retorna Optional.empty() si no existe o está deshabilitada.
     */
    @Transactional(readOnly = true)
    public Optional<String> getDecryptedKey(Long companyId, CompanyApiKey.Provider provider) {
        return apiKeyRepository.findByCompanyIdAndProviderAndEnabledTrue(companyId, provider)
                .map(key -> encryptionService.decrypt(key.getEncryptedKey()));
    }
}
