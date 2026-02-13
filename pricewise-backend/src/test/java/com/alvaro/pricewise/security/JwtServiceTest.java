package com.alvaro.pricewise.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtService Tests")
class JwtServiceTest {

    private JwtService jwtService;

    private static final String TEST_SECRET = "TestSecretKeyQueTieneQueTenerAlMenos256BitsParaQueNoFalle123456";
    private static final long TEST_EXPIRATION = 86400000L; // 24h

    private UserPrincipal testPrincipal;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", TEST_EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "activeProfile", "test");

        testPrincipal = UserPrincipal.builder()
                .id(1L)
                .companyId(1L)
                .email("test@email.com")
                .username("testuser")
                .password("encoded")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_COMPANY_ADMIN")))
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("Generacion de tokens")
    class GenerateTokenTests {

        @Test
        @DisplayName("Genera token no nulo para usuario valido")
        void generateToken_ValidUser_ReturnsToken() {
            String token = jwtService.generateToken(testPrincipal);

            assertNotNull(token);
            assertFalse(token.isEmpty());
        }

        @Test
        @DisplayName("Token contiene el email como subject")
        void generateToken_ContainsEmailAsSubject() {
            String token = jwtService.generateToken(testPrincipal);
            String username = jwtService.extractUsername(token);

            // UserPrincipal.getUsername() devuelve el email
            assertEquals("test@email.com", username);
        }

        @Test
        @DisplayName("Token contiene claims personalizados (companyId, userId)")
        void generateToken_ContainsCustomClaims() {
            String token = jwtService.generateToken(testPrincipal);

            // Extraer claims directamente
            byte[] keyBytes = Decoders.BASE64.decode(TEST_SECRET);
            SecretKey key = Keys.hmacShaKeyFor(keyBytes);
            var claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();

            assertEquals(1L, ((Number) claims.get("companyId")).longValue());
            assertEquals(1L, ((Number) claims.get("userId")).longValue());
        }
    }

    @Nested
    @DisplayName("Validacion de tokens")
    class ValidateTokenTests {

        @Test
        @DisplayName("Token valido se valida correctamente")
        void validateToken_ValidToken_ReturnsTrue() {
            String token = jwtService.generateToken(testPrincipal);

            assertTrue(jwtService.validateToken(token));
        }

        @Test
        @DisplayName("Token valido es valido para el usuario correcto")
        void isTokenValid_CorrectUser_ReturnsTrue() {
            String token = jwtService.generateToken(testPrincipal);

            assertTrue(jwtService.isTokenValid(token, testPrincipal));
        }

        @Test
        @DisplayName("Token valido no es valido para otro usuario")
        void isTokenValid_WrongUser_ReturnsFalse() {
            String token = jwtService.generateToken(testPrincipal);

            UserPrincipal otherUser = UserPrincipal.builder()
                    .id(2L)
                    .companyId(1L)
                    .email("other@email.com")
                    .username("other")
                    .password("enc")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE")))
                    .active(true)
                    .build();

            assertFalse(jwtService.isTokenValid(token, otherUser));
        }

        @Test
        @DisplayName("Token malformado no se valida")
        void validateToken_MalformedToken_ReturnsFalse() {
            assertFalse(jwtService.validateToken("token-invalido-123"));
        }

        @Test
        @DisplayName("Token con firma incorrecta lanza SignatureException")
        void validateToken_WrongSignature_ThrowsException() {
            // Generar una clave distinta pero válida para HS256
            SecretKey otherKey = Jwts.SIG.HS256.key().build();

            String fakeToken = Jwts.builder()
                    .subject("test@email.com")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 86400000))
                    .signWith(otherKey, Jwts.SIG.HS256)
                    .compact();

            // El JwtService no captura SignatureException - esto lanza excepcion
            assertThrows(io.jsonwebtoken.security.SignatureException.class,
                    () -> jwtService.validateToken(fakeToken));
        }

        @Test
        @DisplayName("Token expirado no se valida")
        void validateToken_ExpiredToken_ReturnsFalse() {
            // Crear servicio con expiracion de 0ms
            JwtService expiredService = new JwtService();
            ReflectionTestUtils.setField(expiredService, "secretKey", TEST_SECRET);
            ReflectionTestUtils.setField(expiredService, "jwtExpiration", 0L);
            ReflectionTestUtils.setField(expiredService, "activeProfile", "test");

            String expiredToken = expiredService.generateToken(testPrincipal);

            assertFalse(jwtService.validateToken(expiredToken));
        }

        @Test
        @DisplayName("Token vacio no se valida")
        void validateToken_EmptyToken_ReturnsFalse() {
            assertFalse(jwtService.validateToken(""));
        }
    }

    @Nested
    @DisplayName("Extraccion de datos")
    class ExtractDataTests {

        @Test
        @DisplayName("Extrae username (email) correctamente")
        void extractUsername_ReturnsEmail() {
            String token = jwtService.generateToken(testPrincipal);

            assertEquals("test@email.com", jwtService.extractUsername(token));
        }
    }

    @Nested
    @DisplayName("Validacion de configuracion")
    class ConfigValidationTests {

        @Test
        @DisplayName("Secret demasiado corto lanza excepcion")
        void validateSecretKey_ShortKey_ThrowsException() {
            JwtService shortKeyService = new JwtService();
            ReflectionTestUtils.setField(shortKeyService, "secretKey", "corta");
            ReflectionTestUtils.setField(shortKeyService, "activeProfile", "dev");

            assertThrows(IllegalStateException.class, shortKeyService::validateSecretKey);
        }

        @Test
        @DisplayName("Secret por defecto en produccion lanza excepcion")
        void validateSecretKey_DefaultKeyInProd_ThrowsException() {
            JwtService prodService = new JwtService();
            ReflectionTestUtils.setField(prodService, "secretKey",
                    "MiClaveSecretaSuperSeguraParaPriceWise2026QueTieneQueTenerAlMenos256Bits");
            ReflectionTestUtils.setField(prodService, "activeProfile", "prod");

            assertThrows(IllegalStateException.class, prodService::validateSecretKey);
        }

        @Test
        @DisplayName("Secret personalizado en produccion no lanza excepcion")
        void validateSecretKey_CustomKeyInProd_DoesNotThrow() {
            JwtService prodService = new JwtService();
            ReflectionTestUtils.setField(prodService, "secretKey", TEST_SECRET);
            ReflectionTestUtils.setField(prodService, "activeProfile", "prod");

            assertDoesNotThrow(prodService::validateSecretKey);
        }
    }
}
