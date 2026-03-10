package com.alvaro.pricewise.service;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.alvaro.pricewise.dto.apikey.ApiKeyDTOs.ApiKeyResponse;
import com.alvaro.pricewise.dto.apikey.ApiKeyDTOs.SaveApiKeyRequest;
import com.alvaro.pricewise.entity.Company;
import com.alvaro.pricewise.entity.CompanyApiKey;
import com.alvaro.pricewise.exception.BadRequestException;
import com.alvaro.pricewise.exception.ResourceNotFoundException;
import com.alvaro.pricewise.repository.CompanyApiKeyRepository;
import com.alvaro.pricewise.repository.CompanyRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyApiKeyService Tests")
class CompanyApiKeyServiceTest {

    @Mock private CompanyApiKeyRepository apiKeyRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private ApiKeyEncryptionService encryptionService;

    private CompanyApiKeyService service;
    private Company testCompany;

    @BeforeEach
    void setUp() {
        service = new CompanyApiKeyService(apiKeyRepository, companyRepository, encryptionService);

        testCompany = Company.builder()
                .id(1L)
                .name("Test Company")
                .build();
    }

    private CompanyApiKey buildApiKey(Long id, CompanyApiKey.Provider provider) {
        return CompanyApiKey.builder()
                .id(id)
                .company(testCompany)
                .provider(provider)
                .encryptedKey("encrypted-value")
                .enabled(true)
                .build();
    }

    @Nested
    @DisplayName("getApiKeys")
    class GetApiKeysTests {

        @Test
        @DisplayName("Devuelve keys enmascaradas")
        void returnsMaskedKeys() {
            CompanyApiKey key = buildApiKey(1L, CompanyApiKey.Provider.KEEPA);
            when(apiKeyRepository.findByCompanyIdOrderByProviderAsc(1L)).thenReturn(List.of(key));
            when(encryptionService.decrypt("encrypted-value")).thenReturn("keepa-real-key-1234");
            when(encryptionService.mask("keepa-real-key-1234")).thenReturn("****1234");

            List<ApiKeyResponse> result = service.getApiKeys(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProvider()).isEqualTo("KEEPA");
            assertThat(result.get(0).getMaskedKey()).isEqualTo("****1234");
        }

        @Test
        @DisplayName("Devuelve lista vacia si no hay keys")
        void returnsEmptyList() {
            when(apiKeyRepository.findByCompanyIdOrderByProviderAsc(1L)).thenReturn(List.of());
            assertThat(service.getApiKeys(1L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("saveApiKey")
    class SaveApiKeyTests {

        @Test
        @DisplayName("Crea nueva API key")
        void createsNewKey() {
            when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));
            when(encryptionService.encrypt("my-keepa-key")).thenReturn("encrypted-123");
            when(encryptionService.mask("my-keepa-key")).thenReturn("****-key");
            when(apiKeyRepository.findByCompanyIdAndProvider(1L, CompanyApiKey.Provider.KEEPA))
                    .thenReturn(Optional.empty());
            when(apiKeyRepository.save(any(CompanyApiKey.class))).thenAnswer(inv -> {
                CompanyApiKey k = inv.getArgument(0);
                k.setId(10L);
                return k;
            });

            SaveApiKeyRequest request = SaveApiKeyRequest.builder()
                    .provider("KEEPA")
                    .apiKey("my-keepa-key")
                    .build();

            ApiKeyResponse response = service.saveApiKey(1L, request);

            assertThat(response.getProvider()).isEqualTo("KEEPA");
            assertThat(response.getMaskedKey()).isEqualTo("****-key");

            ArgumentCaptor<CompanyApiKey> captor = ArgumentCaptor.forClass(CompanyApiKey.class);
            verify(apiKeyRepository).save(captor.capture());
            assertThat(captor.getValue().getEncryptedKey()).isEqualTo("encrypted-123");
        }

        @Test
        @DisplayName("Actualiza key existente")
        void updatesExistingKey() {
            CompanyApiKey existing = buildApiKey(5L, CompanyApiKey.Provider.KEEPA);
            when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));
            when(encryptionService.encrypt("new-key")).thenReturn("encrypted-new");
            when(encryptionService.mask("new-key")).thenReturn("****-key");
            when(apiKeyRepository.findByCompanyIdAndProvider(1L, CompanyApiKey.Provider.KEEPA))
                    .thenReturn(Optional.of(existing));
            when(apiKeyRepository.save(any(CompanyApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

            SaveApiKeyRequest request = SaveApiKeyRequest.builder()
                    .provider("KEEPA")
                    .apiKey("new-key")
                    .build();

            ApiKeyResponse response = service.saveApiKey(1L, request);

            assertThat(response.getId()).isEqualTo(5L);
            verify(apiKeyRepository).save(existing);
            assertThat(existing.getEncryptedKey()).isEqualTo("encrypted-new");
            assertThat(existing.getEnabled()).isTrue();
        }

        @Test
        @DisplayName("Rechaza proveedor invalido")
        void rejectsInvalidProvider() {
            when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));

            SaveApiKeyRequest request = SaveApiKeyRequest.builder()
                    .provider("INVALID")
                    .apiKey("some-key")
                    .build();

            assertThatThrownBy(() -> service.saveApiKey(1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Proveedor no soportado");
        }

        @Test
        @DisplayName("Lanza excepcion si empresa no existe")
        void throwsIfCompanyNotFound() {
            when(companyRepository.findById(999L)).thenReturn(Optional.empty());

            SaveApiKeyRequest request = SaveApiKeyRequest.builder()
                    .provider("KEEPA")
                    .apiKey("key")
                    .build();

            assertThatThrownBy(() -> service.saveApiKey(999L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("toggleApiKey")
    class ToggleApiKeyTests {

        @Test
        @DisplayName("Cambia enabled de true a false")
        void togglesToFalse() {
            CompanyApiKey key = buildApiKey(1L, CompanyApiKey.Provider.KEEPA);
            key.setEnabled(true);
            when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(key));
            when(apiKeyRepository.save(any(CompanyApiKey.class))).thenAnswer(inv -> inv.getArgument(0));
            when(encryptionService.decrypt("encrypted-value")).thenReturn("real-key");
            when(encryptionService.mask("real-key")).thenReturn("****-key");

            ApiKeyResponse response = service.toggleApiKey(1L, 1L);

            assertThat(response.getEnabled()).isFalse();
        }

        @Test
        @DisplayName("Lanza excepcion si key de otra empresa")
        void throwsIfKeyFromOtherCompany() {
            Company otherCompany = Company.builder().id(99L).build();
            CompanyApiKey key = CompanyApiKey.builder()
                    .id(1L).company(otherCompany).provider(CompanyApiKey.Provider.KEEPA)
                    .encryptedKey("enc").enabled(true).build();
            when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(key));

            assertThatThrownBy(() -> service.toggleApiKey(1L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteApiKey")
    class DeleteApiKeyTests {

        @Test
        @DisplayName("Elimina key existente")
        void deletesExistingKey() {
            CompanyApiKey key = buildApiKey(1L, CompanyApiKey.Provider.KEEPA);
            when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(key));

            service.deleteApiKey(1L, 1L);

            verify(apiKeyRepository).delete(key);
        }

        @Test
        @DisplayName("Lanza excepcion si key no existe")
        void throwsIfKeyNotFound() {
            when(apiKeyRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteApiKey(1L, 999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getDecryptedKey")
    class GetDecryptedKeyTests {

        @Test
        @DisplayName("Devuelve key descifrada si existe y esta habilitada")
        void returnsDecryptedKey() {
            CompanyApiKey key = buildApiKey(1L, CompanyApiKey.Provider.KEEPA);
            when(apiKeyRepository.findByCompanyIdAndProviderAndEnabledTrue(1L, CompanyApiKey.Provider.KEEPA))
                    .thenReturn(Optional.of(key));
            when(encryptionService.decrypt("encrypted-value")).thenReturn("real-keepa-key");

            Optional<String> result = service.getDecryptedKey(1L, CompanyApiKey.Provider.KEEPA);

            assertThat(result).contains("real-keepa-key");
        }

        @Test
        @DisplayName("Devuelve empty si no existe")
        void returnsEmptyIfNotFound() {
            when(apiKeyRepository.findByCompanyIdAndProviderAndEnabledTrue(1L, CompanyApiKey.Provider.KEEPA))
                    .thenReturn(Optional.empty());

            assertThat(service.getDecryptedKey(1L, CompanyApiKey.Provider.KEEPA)).isEmpty();
        }
    }
}
