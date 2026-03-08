package com.alvaro.pricewise.controller;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alvaro.pricewise.dto.admin.AdminDTOs.StatusChangeRequest;
import com.alvaro.pricewise.dto.admin.AdminDTOs.UpdateUserRequest;
import com.alvaro.pricewise.dto.admin.AdminDTOs.UserDetail;
import com.alvaro.pricewise.dto.admin.AdminDTOs.UserSummary;
import com.alvaro.pricewise.dto.auth.AuthDTOs.CompanyResponse;
import com.alvaro.pricewise.entity.Company;
import com.alvaro.pricewise.entity.User;
import com.alvaro.pricewise.exception.GlobalExceptionHandler;
import com.alvaro.pricewise.exception.ResourceNotFoundException;
import com.alvaro.pricewise.security.JwtService;
import com.alvaro.pricewise.security.UserPrincipal;
import com.alvaro.pricewise.service.AdminService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("AdminController Tests")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        Company testCompany = Company.builder().id(1L).name("Test Co").build();
        User testUser = User.builder()
                .id(1L).username("testuser").email("test@email.com")
                .password("enc").company(testCompany).role(User.Role.ADMIN).active(true)
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
        when(adminService.getAllUsers()).thenReturn(List.of(
                new UserSummary(1L, "testuser", "test@email.com", "Test Co", "ADMIN", true, 0L)));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].username").value("testuser"))
                .andExpect(jsonPath("$.data[0].email").value("test@email.com"));
    }

    @Test
    @DisplayName("GET /stats devuelve estadisticas")
    void getStats_returnsStatisticsMap() throws Exception {
        when(adminService.getStats()).thenReturn(Map.of(
                "totalUsers", 5L, "activeUsers", 4L, "totalCompanies", 3L));

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
        when(adminService.getUser(1L)).thenReturn(
                new UserDetail(1L, "testuser", "test@email.com", "Test Co", null, "ADMIN", true, null, null));

        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.companyName").value("Test Co"));
    }

    @Test
    @DisplayName("GET /companies devuelve lista de empresas")
    void getCompanies_returnsList() throws Exception {
        when(adminService.getAllCompanies()).thenReturn(List.of(
                CompanyResponse.builder().id(1L).name("Test Co").build()));

        mockMvc.perform(get("/api/admin/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Test Co"));
    }

    @Test
    @DisplayName("GET /companies/{id} devuelve detalle de empresa")
    void getCompany_returnsDetail() throws Exception {
        when(adminService.getCompany(1L)).thenReturn(
                CompanyResponse.builder().id(1L).name("Test Co").build());

        mockMvc.perform(get("/api/admin/companies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Test Co"));
    }

    @Test
    @DisplayName("PUT /users/{id} actualiza usuario correctamente")
    void updateUser_updatesSuccessfully() throws Exception {
        when(adminService.updateUser(eq(1L), any(UpdateUserRequest.class))).thenReturn(
                new UserDetail(1L, "newname", "test@email.com", "Test Co", null, "ADMIN", true, null, null));

        UpdateUserRequest request = new UpdateUserRequest("newname", null, null, null);

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
        when(adminService.changeUserStatus(1L, false)).thenReturn(
                new UserSummary(1L, "testuser", "test@email.com", "Test Co", "ADMIN", false, 0L));

        StatusChangeRequest request = new StatusChangeRequest(false);

        mockMvc.perform(put("/api/admin/users/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE /users/{id} elimina usuario")
    void deleteUser_deletesSuccessfully() throws Exception {
        doNothing().when(adminService).deleteUser(1L);

        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Usuario desactivado"));

        verify(adminService).deleteUser(1L);
    }

    @Test
    @DisplayName("PUT /users/{id} con usuario inexistente devuelve 404")
    void updateUser_nonExistentUser_returns404() throws Exception {
        when(adminService.updateUser(eq(999L), any(UpdateUserRequest.class)))
                .thenThrow(new ResourceNotFoundException("Usuario no encontrado"));

        UpdateUserRequest request = new UpdateUserRequest("newname", null, null, null);

        mockMvc.perform(put("/api/admin/users/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
