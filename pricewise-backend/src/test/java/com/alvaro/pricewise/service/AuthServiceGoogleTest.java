package com.alvaro.pricewise.service;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.alvaro.pricewise.dto.auth.AuthDTOs.*;
import com.alvaro.pricewise.entity.Company;
import com.alvaro.pricewise.entity.User;
import com.alvaro.pricewise.exception.BadRequestException;
import com.alvaro.pricewise.repository.CompanyRepository;
import com.alvaro.pricewise.repository.ProductRepository;
import com.alvaro.pricewise.repository.UserRepository;
import com.alvaro.pricewise.security.JwtService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Google OAuth2 Tests")
class AuthServiceGoogleTest {

    @Mock private UserRepository userRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private ProductRepository productRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private GoogleTokenService googleTokenService;

    private AuthService authService;
    private Company testCompany;

    private GoogleIdToken.Payload createPayload(String email, String name) {
        GoogleIdToken.Payload p = new GoogleIdToken.Payload();
        p.setEmail(email);
        p.setEmailVerified(true);
        p.set("name", name);
        return p;
    }

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, companyRepository, productRepository,
                passwordEncoder, jwtService, authenticationManager, googleTokenService);

        testCompany = Company.builder()
                .id(1L)
                .name("Test Company")
                .companyCode("ABCD1234")
                .businessType("ecommerce")
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("googleLogin")
    class GoogleLoginTests {

        @Test
        @DisplayName("Usuario existente devuelve AUTHENTICATED con token")
        void existingUserReturnsAuthenticated() {
            GoogleIdToken.Payload p = createPayload("user@google.com", "Google User");
            when(googleTokenService.verify("valid-token")).thenReturn(p);

            User existingUser = User.builder()
                    .id(1L)
                    .username("googleuser")
                    .email("user@google.com")
                    .password("hashed")
                    .company(testCompany)
                    .role(User.Role.EMPLOYEE)
                    .build();

            when(userRepository.findByEmail("user@google.com")).thenReturn(Optional.of(existingUser));
            when(jwtService.generateToken(any())).thenReturn("jwt-token");

            GoogleLoginResponse response = authService.googleLogin(
                    GoogleLoginRequest.builder().idToken("valid-token").build());

            assertThat(response.getStatus()).isEqualTo("AUTHENTICATED");
            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getEmail()).isEqualTo("user@google.com");
            assertThat(response.getCompanyId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Usuario nuevo devuelve NEEDS_SETUP sin token")
        void newUserReturnsNeedsSetup() {
            GoogleIdToken.Payload p = createPayload("new@google.com", "New User");
            when(googleTokenService.verify("valid-token")).thenReturn(p);
            when(userRepository.findByEmail("new@google.com")).thenReturn(Optional.empty());

            GoogleLoginResponse response = authService.googleLogin(
                    GoogleLoginRequest.builder().idToken("valid-token").build());

            assertThat(response.getStatus()).isEqualTo("NEEDS_SETUP");
            assertThat(response.getToken()).isNull();
            assertThat(response.getGoogleEmail()).isEqualTo("new@google.com");
            assertThat(response.getGoogleName()).isEqualTo("New User");
        }
    }

    @Nested
    @DisplayName("googleCompleteNewCompany")
    class GoogleCompleteNewCompanyTests {

        @Test
        @DisplayName("Crea empresa y usuario COMPANY_ADMIN")
        void createsCompanyAndAdmin() {
            GoogleIdToken.Payload p = createPayload("admin@google.com", "Admin User");
            when(googleTokenService.verify("valid-token")).thenReturn(p);
            when(userRepository.existsByEmail("admin@google.com")).thenReturn(false);
            when(userRepository.existsByUsername("adminuser")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed-random");
            when(companyRepository.save(any(Company.class))).thenAnswer(inv -> {
                Company c = inv.getArgument(0);
                c.setId(10L);
                return c;
            });
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(5L);
                return u;
            });
            when(jwtService.generateToken(any())).thenReturn("jwt-token");

            GoogleCompleteNewCompanyRequest request = GoogleCompleteNewCompanyRequest.builder()
                    .googleIdToken("valid-token")
                    .companyName("Mi Empresa")
                    .businessType("retail")
                    .build();

            AuthResponse response = authService.googleCompleteNewCompany(request);

            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getRole()).isEqualTo("COMPANY_ADMIN");
            assertThat(response.getEmail()).isEqualTo("admin@google.com");
            verify(companyRepository).save(any(Company.class));
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Rechaza si email ya existe")
        void rejectsExistingEmail() {
            GoogleIdToken.Payload p = createPayload("existing@google.com", "Existing User");
            when(googleTokenService.verify("valid-token")).thenReturn(p);
            when(userRepository.existsByEmail("existing@google.com")).thenReturn(true);

            GoogleCompleteNewCompanyRequest request = GoogleCompleteNewCompanyRequest.builder()
                    .googleIdToken("valid-token")
                    .companyName("Mi Empresa")
                    .build();

            assertThatThrownBy(() -> authService.googleCompleteNewCompany(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("email ya esta registrado");
        }
    }

    @Nested
    @DisplayName("googleCompleteJoin")
    class GoogleCompleteJoinTests {

        @Test
        @DisplayName("Une usuario a empresa existente como EMPLOYEE")
        void joinsExistingCompany() {
            GoogleIdToken.Payload p = createPayload("employee@google.com", "Employee User");
            when(googleTokenService.verify("valid-token")).thenReturn(p);
            when(userRepository.existsByEmail("employee@google.com")).thenReturn(false);
            when(userRepository.existsByUsername("employeeuser")).thenReturn(false);
            when(companyRepository.findByCompanyCode("ABCD1234")).thenReturn(Optional.of(testCompany));
            when(passwordEncoder.encode(anyString())).thenReturn("hashed-random");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(6L);
                return u;
            });
            when(jwtService.generateToken(any())).thenReturn("jwt-token");

            GoogleCompleteJoinRequest request = GoogleCompleteJoinRequest.builder()
                    .googleIdToken("valid-token")
                    .companyCode("abcd1234")
                    .build();

            AuthResponse response = authService.googleCompleteJoin(request);

            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getRole()).isEqualTo("EMPLOYEE");
            assertThat(response.getCompanyId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Rechaza codigo de empresa invalido")
        void rejectsInvalidCompanyCode() {
            GoogleIdToken.Payload p = createPayload("user@google.com", "Some User");
            when(googleTokenService.verify("valid-token")).thenReturn(p);
            when(userRepository.existsByEmail("user@google.com")).thenReturn(false);
            when(companyRepository.findByCompanyCode("INVALID1")).thenReturn(Optional.empty());

            GoogleCompleteJoinRequest request = GoogleCompleteJoinRequest.builder()
                    .googleIdToken("valid-token")
                    .companyCode("INVALID1")
                    .build();

            assertThatThrownBy(() -> authService.googleCompleteJoin(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("empresa no valido");
        }
    }
}
