package com.alvaro.pricewise.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.alvaro.pricewise.dto.auth.AuthDTOs.*;
import com.alvaro.pricewise.dto.auth.AuthDTOs.AuthResponse;
import com.alvaro.pricewise.dto.auth.AuthDTOs.CreateEmployeeRequest;
import com.alvaro.pricewise.dto.auth.AuthDTOs.LoginRequest;
import com.alvaro.pricewise.dto.auth.AuthDTOs.RegisterRequest;
import com.alvaro.pricewise.dto.auth.AuthDTOs.UserProfileResponse;
import com.alvaro.pricewise.entity.Company;
import com.alvaro.pricewise.entity.User;
import com.alvaro.pricewise.exception.BadRequestException;
import com.alvaro.pricewise.repository.CompanyRepository;
import com.alvaro.pricewise.repository.ProductRepository;
import com.alvaro.pricewise.repository.UserRepository;
import com.alvaro.pricewise.security.JwtService;
import com.alvaro.pricewise.security.UserPrincipal;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private GoogleTokenService googleTokenService;

    private AuthService authService;

    private Company testCompany;
    private User testUser;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, companyRepository, productRepository,
                passwordEncoder, jwtService, authenticationManager, googleTokenService);

        testCompany = Company.builder()
                .id(1L)
                .name("Test Company")
                .businessType("ecommerce")
                .build();

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@email.com")
                .password("encodedPassword")
                .company(testCompany)
                .role(User.Role.COMPANY_ADMIN)
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("Registro")
    class RegisterTests {

        @Test
        @DisplayName("Registro exitoso vincula usuario a empresa existente")
        void register_Success_LinksUserToExistingCompany() {
            RegisterRequest request = RegisterRequest.builder()
                    .username("newuser")
                    .email("new@email.com")
                    .password("Password1")
                    .companyCode("A3F7B2C9")
                    .build();

            when(userRepository.existsByEmail("new@email.com")).thenReturn(false);
            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(companyRepository.findByCompanyCode("A3F7B2C9")).thenReturn(Optional.of(testCompany));
            when(passwordEncoder.encode("Password1")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("jwt-token");

            AuthResponse response = authService.register(request);

            assertNotNull(response);
            assertEquals("jwt-token", response.getToken());
            assertEquals("Bearer", response.getType());
            assertEquals("testuser", response.getUsername());

            verify(companyRepository, never()).save(any(Company.class));
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Registro con email duplicado lanza excepcion")
        void register_DuplicateEmail_ThrowsBadRequest() {
            RegisterRequest request = RegisterRequest.builder()
                    .username("newuser")
                    .email("existing@email.com")
                    .password("Password1")
                    .companyCode("A3F7B2C9")
                    .build();

            when(userRepository.existsByEmail("existing@email.com")).thenReturn(true);

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> authService.register(request));
            assertEquals("El email ya esta registrado", ex.getMessage());

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Registro con username duplicado lanza excepcion")
        void register_DuplicateUsername_ThrowsBadRequest() {
            RegisterRequest request = RegisterRequest.builder()
                    .username("existing")
                    .email("new@email.com")
                    .password("Password1")
                    .companyCode("A3F7B2C9")
                    .build();

            when(userRepository.existsByEmail("new@email.com")).thenReturn(false);
            when(userRepository.existsByUsername("existing")).thenReturn(true);

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> authService.register(request));
            assertEquals("El nombre de usuario ya esta en uso", ex.getMessage());
        }

        @Test
        @DisplayName("Registro con código de empresa inválido lanza excepcion")
        void register_InvalidCompanyCode_ThrowsBadRequest() {
            RegisterRequest request = RegisterRequest.builder()
                    .username("newuser")
                    .email("new@email.com")
                    .password("Password1")
                    .companyCode("INVALID1")
                    .build();

            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(userRepository.existsByUsername(any())).thenReturn(false);
            when(companyRepository.findByCompanyCode("INVALID1")).thenReturn(Optional.empty());

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> authService.register(request));
            assertEquals("Código de empresa no válido", ex.getMessage());
        }

        @Test
        @DisplayName("Registro codifica la password correctamente")
        void register_EncodesPassword() {
            RegisterRequest request = RegisterRequest.builder()
                    .username("newuser")
                    .email("new@email.com")
                    .password("Password1")
                    .companyCode("A3F7B2C9")
                    .build();

            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(userRepository.existsByUsername(any())).thenReturn(false);
            when(companyRepository.findByCompanyCode("A3F7B2C9")).thenReturn(Optional.of(testCompany));
            when(passwordEncoder.encode("Password1")).thenReturn("bcrypt-encoded");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtService.generateToken(any())).thenReturn("token");

            authService.register(request);

            verify(passwordEncoder).encode("Password1");
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertEquals("bcrypt-encoded", captor.getValue().getPassword());
        }
    }

    @Nested
    @DisplayName("Login")
    class LoginTests {

        @Test
        @DisplayName("Login exitoso devuelve token")
        void login_Success_ReturnsToken() {
            LoginRequest request = LoginRequest.builder()
                    .emailOrUsername("test@email.com")
                    .password("Password1")
                    .build();

            UserPrincipal principal = UserPrincipal.create(testUser);
            Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("jwt-token");

            AuthResponse response = authService.login(request);

            assertNotNull(response);
            assertEquals("jwt-token", response.getToken());
            assertEquals("test@email.com", response.getEmail());
        }

        @Test
        @DisplayName("Login con credenciales invalidas lanza excepcion")
        void login_BadCredentials_ThrowsException() {
            LoginRequest request = LoginRequest.builder()
                    .emailOrUsername("test@email.com")
                    .password("WrongPassword")
                    .build();

            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThrows(BadCredentialsException.class,
                    () -> authService.login(request));
        }
    }

    @Nested
    @DisplayName("Perfil")
    class GetProfileTests {

        @Test
        @DisplayName("Obtener perfil devuelve datos correctos")
        void getProfile_ValidUser_ReturnsProfile() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(productRepository.countByCompanyId(1L)).thenReturn(10L);

            UserProfileResponse profile = authService.getProfile(1L);

            assertEquals("testuser", profile.getUsername());
            assertEquals("test@email.com", profile.getEmail());
            assertEquals("Test Company", profile.getCompanyName());
            assertEquals("COMPANY_ADMIN", profile.getRole());
            assertEquals(10L, profile.getTotalProducts());
        }

        @Test
        @DisplayName("Obtener perfil de usuario inexistente lanza excepcion")
        void getProfile_NonExistentUser_ThrowsException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(BadRequestException.class,
                    () -> authService.getProfile(999L));
        }
    }

    @Nested
    @DisplayName("Crear empleado")
    class CreateEmployeeTests {

        @Test
        @DisplayName("Crear empleado exitoso")
        void createEmployee_Success() {
            CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                    .username("empleado1")
                    .email("emp@email.com")
                    .password("Password1")
                    .build();

            User employee = User.builder()
                    .id(2L)
                    .username("empleado1")
                    .email("emp@email.com")
                    .password("enc")
                    .company(testCompany)
                    .role(User.Role.EMPLOYEE)
                    .active(true)
                    .build();

            when(userRepository.existsByEmail("emp@email.com")).thenReturn(false);
            when(userRepository.existsByUsername("empleado1")).thenReturn(false);
            when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));
            when(passwordEncoder.encode("Password1")).thenReturn("enc");
            when(userRepository.save(any(User.class))).thenReturn(employee);
            when(jwtService.generateToken(any())).thenReturn("emp-token");

            AuthResponse response = authService.createEmployee(1L, request);

            assertEquals("emp-token", response.getToken());
            assertEquals("EMPLOYEE", response.getRole());
            assertEquals("empleado1", response.getUsername());
        }

        @Test
        @DisplayName("Crear empleado con email duplicado lanza excepcion")
        void createEmployee_DuplicateEmail_ThrowsException() {
            CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                    .username("emp")
                    .email("existing@email.com")
                    .password("Password1")
                    .build();

            when(userRepository.existsByEmail("existing@email.com")).thenReturn(true);

            assertThrows(BadRequestException.class,
                    () -> authService.createEmployee(1L, request));
        }

        @Test
        @DisplayName("Crear empleado con empresa inexistente lanza excepcion")
        void createEmployee_CompanyNotFound_ThrowsException() {
            CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                    .username("emp")
                    .email("emp@email.com")
                    .password("Password1")
                    .build();

            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(userRepository.existsByUsername(any())).thenReturn(false);
            when(companyRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(BadRequestException.class,
                    () -> authService.createEmployee(999L, request));
        }
    }
}
