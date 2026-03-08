package com.alvaro.pricewise.controller;

import com.alvaro.pricewise.dto.analytics.AnalyticsDTOs.DashboardMetrics;
import com.alvaro.pricewise.dto.analytics.AnalyticsDTOs.RecommendationSummary;
import com.alvaro.pricewise.dto.analytics.AnalyticsDTOs.AlertSummary;
import com.alvaro.pricewise.dto.common.PageResponse;
import com.alvaro.pricewise.exception.GlobalExceptionHandler;
import com.alvaro.pricewise.security.JwtService;
import com.alvaro.pricewise.security.UserPrincipal;
import com.alvaro.pricewise.service.AnalyticsService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("AnalyticsController Tests")
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        UserPrincipal userPrincipal = UserPrincipal.builder()
                .id(1L)
                .companyId(1L)
                .email("user@email.com")
                .username("testuser")
                .password("password")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_COMPANY_ADMIN")))
                .active(true)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("GET /api/analytics/dashboard")
    class GetDashboard {

        @Test
        @DisplayName("Devuelve metricas del dashboard")
        void shouldReturnDashboardMetrics() throws Exception {
            when(analyticsService.getDashboardMetrics(1L)).thenReturn(
                    DashboardMetrics.builder()
                            .totalProducts(10)
                            .activeMonitoring(3)
                            .pendingRecommendations(5)
                            .unreadAlerts(3)
                            .potentialSavings(new BigDecimal("150.00"))
                            .topRecommendations(Collections.emptyList())
                            .alertsByType(Collections.emptyMap())
                            .build());

            mockMvc.perform(get("/api/analytics/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalProducts").value(10))
                    .andExpect(jsonPath("$.data.pendingRecommendations").value(5))
                    .andExpect(jsonPath("$.data.unreadAlerts").value(3));
        }
    }

    @Nested
    @DisplayName("GET /api/analytics/recommendations")
    class GetRecommendations {

        @Test
        @DisplayName("Devuelve lista de recomendaciones paginada")
        void shouldReturnRecommendations() throws Exception {
            when(analyticsService.getRecommendations(1L, 0, 20)).thenReturn(
                    PageResponse.<RecommendationSummary>builder()
                            .content(Collections.emptyList())
                            .pageNumber(0).pageSize(20).totalElements(0).totalPages(0)
                            .first(true).last(true).hasNext(false).hasPrevious(false)
                            .build());

            mockMvc.perform(get("/api/analytics/recommendations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray());
        }
    }

    @Nested
    @DisplayName("POST /api/analytics/recommendations/{id}/apply")
    class ApplyRecommendation {

        @Test
        @DisplayName("Aplica recomendacion correctamente")
        void shouldApplyRecommendation() throws Exception {
            doNothing().when(analyticsService).applyRecommendation(1L, 1L);

            mockMvc.perform(post("/api/analytics/recommendations/1/apply"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("POST /api/analytics/recommendations/{id}/dismiss")
    class DismissRecommendation {

        @Test
        @DisplayName("Descarta recomendacion correctamente")
        void shouldDismissRecommendation() throws Exception {
            doNothing().when(analyticsService).dismissRecommendation(1L, 1L);

            mockMvc.perform(post("/api/analytics/recommendations/1/dismiss"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/analytics/alerts")
    class GetAlerts {

        @Test
        @DisplayName("Devuelve lista de alertas paginada")
        void shouldReturnAlerts() throws Exception {
            when(analyticsService.getAlerts(1L, 0, 20, false)).thenReturn(
                    PageResponse.<AlertSummary>builder()
                            .content(Collections.emptyList())
                            .pageNumber(0).pageSize(20).totalElements(0).totalPages(0)
                            .first(true).last(true).hasNext(false).hasPrevious(false)
                            .build());

            mockMvc.perform(get("/api/analytics/alerts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray());
        }
    }

    @Nested
    @DisplayName("POST /api/analytics/alerts/{id}/read")
    class MarkAlertAsRead {

        @Test
        @DisplayName("Marca alerta como leida")
        void shouldMarkAlertAsRead() throws Exception {
            doNothing().when(analyticsService).markAlertAsRead(1L, 1L);

            mockMvc.perform(post("/api/analytics/alerts/1/read"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("POST /api/analytics/analyze")
    class RunAnalysis {

        @Test
        @DisplayName("Ejecuta analisis y devuelve resultado")
        void shouldRunAnalysis() throws Exception {
            when(analyticsService.analyzeAllProducts(1L)).thenReturn(5);

            mockMvc.perform(post("/api/analytics/analyze"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.productsAnalyzed").value(5));
        }
    }
}
