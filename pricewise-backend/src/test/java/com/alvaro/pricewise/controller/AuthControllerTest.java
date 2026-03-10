package com.alvaro.pricewise.controller;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alvaro.pricewise.dto.auth.AuthDTOs.AuthResponse;
import com.alvaro.pricewise.dto.auth.AuthDTOs.CreateEmployeeRequest;
import com.alvaro.pricewise.dto.auth.AuthDTOs.LoginRequest;
import com.alvaro.pricewise.dto.auth.AuthDTOs.RegisterRequest;
import com.alvaro.pricewise.exception.BadRequestException;
import com.alvaro.pricewise.exception.GlobalExceptionHandler;
import com.alvaro.pricewise.security.JwtService;
import com.alvaro.pricewise.security.UserPrincipal;
import com.alvaro.pricewise.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("AuthController Tests")
@SuppressWarnings("null")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private com.alvaro.pricewise.service.GoogleAuthService googleAuthService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    private static final String BASE_URL = "/api/auth";

    private AuthResponse buildAuthResponse() {
        return AuthResponse.of("jwt-token-123", 1L, "testuser", "test@email.com",
                "COMPANY_ADMIN", 1L, "Test Company");
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Registro exitoso devuelve 201 y token")
        void register_Success_Returns201() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .username("testuser")
                    .email("test@email.com")
                    .password("Password1")
                    .companyCode("A3F7B2C9")
                    .build();

            when(authService.register(any(RegisterRequest.class))).thenReturn(buildAuthResponse());

            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.token").value("jwt-token-123"))
                    .andExpect(jsonPath("$.data.username").value("testuser"))
                    .andExpect(jsonPath("$.data.email").value("test@email.com"))
                    .andExpect(jsonPath("$.data.role").value("COMPANY_ADMIN"))
                    .andExpect(jsonPath("$.data.companyName").value("Test Company"))
                    .andExpect(jsonPath("$.message").value("Usuario registrado exitosamente"));
        }

        @Test
        @DisplayName("Registro con email duplicado devuelve 400")
        void register_DuplicateEmail_Returns400() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .username("testuser")
                    .email("existing@email.com")
                    .password("Password1")
                    .companyCode("A3F7B2C9")
                    .build();

            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new BadRequestException("El email ya esta registrado"));

            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("El email ya esta registrado"));
        }

        @Test
        @DisplayName("Registro con username duplicado devuelve 400")
        void register_DuplicateUsername_Returns400() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .username("existing")
                    .email("new@email.com")
                    .password("Password1")
                    .companyCode("A3F7B2C9")
                    .build();

            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new BadRequestException("El nombre de usuario ya esta en uso"));

            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("El nombre de usuario ya esta en uso"));
        }

        @Test
        @DisplayName("Registro sin email devuelve 400 con error de validacion")
        void register_MissingEmail_Returns400() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .username("testuser")
                    .password("Password1")
                    .companyCode("A3F7B2C9")
                    .build();

            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Registro sin username devuelve 400")
        void register_MissingUsername_Returns400() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .email("test@email.com")
                    .password("Password1")
                    .companyCode("A3F7B2C9")
                    .build();

            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Registro sin password devuelve 400")
        void register_MissingPassword_Returns400() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .username("testuser")
                    .email("test@email.com")
                    .companyCode("A3F7B2C9")
                    .build();

            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Registro con password debil devuelve 400")
        void register_WeakPassword_Returns400() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .username("testuser")
                    .email("test@email.com")
                    .password("weak")
                    .build();

            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Registro con email invalido devuelve 400")
        void register_InvalidEmail_Returns400() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .username("testuser")
                    .email("no-es-un-email")
                    .password("Password1")
                    .companyCode("A3F7B2C9")
                    .build();

            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Registro con body vacio devuelve 400")
        void register_EmptyBody_Returns400() throws Exception {
            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("Login exitoso devuelve 200 y token")
        void login_Success_Returns200() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .emailOrUsername("test@email.com")
                    .password("Password1")
                    .build();

            when(authService.login(any(LoginRequest.class))).thenReturn(buildAuthResponse());

            mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.token").value("jwt-token-123"))
                    .andExpect(jsonPath("$.data.type").value("Bearer"))
                    .andExpect(jsonPath("$.data.userId").value(1))
                    .andExpect(jsonPath("$.message").value("Login exitoso"));
        }

        @Test
        @DisplayName("Login con credenciales invalidas devuelve 401")
        void login_BadCredentials_Returns401() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .emailOrUsername("test@email.com")
                    .password("WrongPassword1")
                    .build();

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Login sin emailOrUsername devuelve 400")
        void login_MissingEmailOrUsername_Returns400() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .password("Password1")
                    .build();

            mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Login sin password devuelve 400")
        void login_MissingPassword_Returns400() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .emailOrUsername("test@email.com")
                    .build();

            mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/create-employee")
    class CreateEmployeeTests {

        private void setAdminAuth() {
            UserPrincipal principal = UserPrincipal.builder()
                    .id(1L)
                    .companyId(1L)
                    .email("admin@email.com")
                    .username("admin")
                    .password("encoded")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_COMPANY_ADMIN")))
                    .active(true)
                    .build();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        }

        @AfterEach
        void clearAuth() {
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("Crear empleado exitoso devuelve 201")
        void createEmployee_Success_Returns201() throws Exception {
            setAdminAuth();

            CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                    .username("empleado1")
                    .email("empleado@email.com")
                    .password("Password1")
                    .build();

            AuthResponse response = AuthResponse.of("jwt-emp-token", 2L, "empleado1",
                    "empleado@email.com", "EMPLOYEE", 1L, "Test Company");

            when(authService.createEmployee(any(), any(), any(CreateEmployeeRequest.class))).thenReturn(response);

            mockMvc.perform(post(BASE_URL + "/create-employee")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.role").value("EMPLOYEE"))
                    .andExpect(jsonPath("$.data.username").value("empleado1"))
                    .andExpect(jsonPath("$.message").value("Empleado creado exitosamente"));
        }

        @Test
        @DisplayName("Crear empleado con email duplicado devuelve 400")
        void createEmployee_DuplicateEmail_Returns400() throws Exception {
            setAdminAuth();

            CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                    .username("empleado1")
                    .email("existing@email.com")
                    .password("Password1")
                    .build();

            when(authService.createEmployee(any(), any(), any(CreateEmployeeRequest.class)))
                    .thenThrow(new BadRequestException("El email ya esta registrado"));

            mockMvc.perform(post(BASE_URL + "/create-employee")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("El email ya esta registrado"));
        }

        @Test
        @DisplayName("Crear empleado sin campos requeridos devuelve 400")
        void createEmployee_MissingFields_Returns400() throws Exception {
            setAdminAuth();

            mockMvc.perform(post(BASE_URL + "/create-employee")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
