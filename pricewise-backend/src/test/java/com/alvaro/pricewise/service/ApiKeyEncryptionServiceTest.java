package com.alvaro.pricewise.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ApiKeyEncryptionService Tests")
class ApiKeyEncryptionServiceTest {

    private ApiKeyEncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new ApiKeyEncryptionService("PriceWiseDevKey!PriceWiseDevKey!");
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("Acepta key de 32 caracteres")
        void accepts32CharKey() {
            new ApiKeyEncryptionService("12345678901234567890123456789012");
        }

        @Test
        @DisplayName("Rechaza key de longitud incorrecta")
        void rejectsWrongLengthKey() {
            assertThatThrownBy(() -> new ApiKeyEncryptionService("short"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("32 caracteres");
        }

        @Test
        @DisplayName("Usa key por defecto si esta vacia")
        void usesDefaultKeyWhenBlank() {
            ApiKeyEncryptionService service = new ApiKeyEncryptionService("");
            String encrypted = service.encrypt("test");
            assertThat(service.decrypt(encrypted)).isEqualTo("test");
        }
    }

    @Nested
    @DisplayName("encrypt/decrypt")
    class EncryptDecryptTests {

        @Test
        @DisplayName("Cifra y descifra correctamente")
        void encryptDecryptRoundTrip() {
            String original = "keepa-api-key-12345";
            String encrypted = encryptionService.encrypt(original);

            assertThat(encrypted).isNotEqualTo(original);
            assertThat(encryptionService.decrypt(encrypted)).isEqualTo(original);
        }

        @Test
        @DisplayName("Produce resultado diferente cada vez (IV aleatorio)")
        void producesUniqueEncryptions() {
            String original = "same-key";
            String encrypted1 = encryptionService.encrypt(original);
            String encrypted2 = encryptionService.encrypt(original);

            assertThat(encrypted1).isNotEqualTo(encrypted2);
            assertThat(encryptionService.decrypt(encrypted1)).isEqualTo(original);
            assertThat(encryptionService.decrypt(encrypted2)).isEqualTo(original);
        }

        @Test
        @DisplayName("Maneja strings vacios")
        void handlesEmptyString() {
            String encrypted = encryptionService.encrypt("");
            assertThat(encryptionService.decrypt(encrypted)).isEmpty();
        }

        @Test
        @DisplayName("Maneja caracteres especiales y unicode")
        void handlesSpecialCharacters() {
            String original = "key-with-spëcial-chars!@#$%^&*()_+-={}[]|\\:\";<>?,./~`";
            String encrypted = encryptionService.encrypt(original);
            assertThat(encryptionService.decrypt(encrypted)).isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("mask")
    class MaskTests {

        @Test
        @DisplayName("Muestra ultimos 4 caracteres")
        void showsLast4Chars() {
            assertThat(encryptionService.mask("keepa-api-key-12345")).isEqualTo("****2345");
        }

        @Test
        @DisplayName("Devuelve **** para strings cortos")
        void masksShortStrings() {
            assertThat(encryptionService.mask("ab")).isEqualTo("****");
            assertThat(encryptionService.mask("abcd")).isEqualTo("****");
        }

        @Test
        @DisplayName("Devuelve **** para null")
        void masksNull() {
            assertThat(encryptionService.mask(null)).isEqualTo("****");
        }
    }
}
