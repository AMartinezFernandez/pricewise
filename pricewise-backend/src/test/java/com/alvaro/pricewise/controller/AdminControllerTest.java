package com.alvaro.pricewise.controller;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alvaro.pricewise.entity.Company;
import com.alvaro.pricewise.entity.User;
import com.alvaro.pricewise.exception.GlobalExceptionHandler;
import com.alvaro.pricewise.repository.CompetitorRepository;
import com.alvaro.pricewise.repository.ProductRepository;
import com.alvaro.pricewise.repository.UserRepository;
import com.alvaro.pricewise.security.JwtService;
import com.alvaro.pricewise.security.UserPrincipal;
import com.alvaro.pricewise.service.AuthService;
import com.alvaro.pricewise.service.KeepaService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("AdminController Tests")
@SuppressWarnings("null")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private CompetitorRepository competitorRepository;

    @MockBean
    private com.alvaro.pricewise.repository.CompanyRepository companyRepository;

    @MockBean
    private KeepaService keepaService;

    @MockBean
    private AuthService authService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private Scheduler scheduler;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    private User testUser;
    private Company testCompany;

    @BeforeEach
    void setUp() {
        testCompany = Company.builder()
                .id(1L)
                .name("Test Co")
                .build();

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@email.com")
                .password("enc")
                .company(testCompany)
                .role(User.Role.ADMIN)
                .active(true)
                .build();

        UserPrincipal adminPrincipal = UserPrincipal.create(testUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /users devuelve lista de usuarios")
    void getUsers_returnsList() throws Exception {
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].username").value("testuser"))
                .andExpect(jsonPath("$.data[0].email").value("test@email.com"));
    }

    @Test
    @DisplayName("GET /stats devuelve estadisticas")
    void getStats_returnsStatisticsMap() throws Exception {
        when(userRepository.count()).thenReturn(5L);
        when(userRepository.countByActiveTrue()).thenReturn(4L);
        when(userRepository.countByRole(User.Role.ADMIN)).thenReturn(1L);
        when(userRepository.countByRole(User.Role.COMPANY_ADMIN)).thenReturn(2L);
        when(userRepository.countByRole(User.Role.EMPLOYEE)).thenReturn(2L);
        when(companyRepository.count()).thenReturn(3L);

        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").value(5))
                .andExpect(jsonPath("$.data.activeUsers").value(4))
                .andExpect(jsonPath("$.data.totalCompanies").value(3));
    }

    @Test
    @DisplayName("GET /users/{id} devuelve detalle de usuario")
    void getUser_returnsDetail() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.companyName").value("Test Co"));
    }

    @Test
    @DisplayName("GET /companies devuelve lista de empresas")
    void getCompanies_returnsList() throws Exception {
        when(companyRepository.findAll()).thenReturn(List.of(testCompany));

        mockMvc.perform(get("/api/admin/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Test Co"));
    }

    @Test
    @DisplayName("GET /companies/{id} devuelve detalle de empresa")
    void getCompany_returnsDetail() throws Exception {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));

        mockMvc.perform(get("/api/admin/companies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Test Co"));
    }

    @Test
    @DisplayName("PUT /users/{id} actualiza usuario correctamente")
    void updateUser_updatesSuccessfully() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        AdminController.UpdateUserRequest request =
                new AdminController.UpdateUserRequest("newname", null, null, null);

        mockMvc.perform(put("/api/admin/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Usuario actualizado correctamente"));
    }

    @Test
    @DisplayName("PUT /users/{id}/status cambia estado del usuario")
    void changeStatus_changesSuccessfully() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        AdminController.StatusChangeRequest request =
                new AdminController.StatusChangeRequest(false);

        mockMvc.perform(put("/api/admin/users/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE /users/{id} elimina usuario")
    void deleteUser_deletesSuccessfully() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Usuario eliminado permanentemente"));

        verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("PUT /users/{id} con usuario inexistente devuelve 404")
    void updateUser_nonExistentUser_returns404() throws Exception {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        AdminController.UpdateUserRequest request =
                new AdminController.UpdateUserRequest("newname", null, null, null);

        mockMvc.perform(put("/api/admin/users/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
