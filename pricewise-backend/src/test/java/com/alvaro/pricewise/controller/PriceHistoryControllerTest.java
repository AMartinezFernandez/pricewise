package com.alvaro.pricewise.controller;

import com.alvaro.pricewise.dto.common.PageResponse;
import com.alvaro.pricewise.dto.history.PriceHistoryDTOs.PriceHistoryResponse;
import com.alvaro.pricewise.exception.GlobalExceptionHandler;
import com.alvaro.pricewise.exception.ResourceNotFoundException;
import com.alvaro.pricewise.security.JwtService;
import com.alvaro.pricewise.security.UserPrincipal;
import com.alvaro.pricewise.service.PriceHistoryService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PriceHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("PriceHistoryController Tests")
class PriceHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PriceHistoryService priceHistoryService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    private static final Long PRODUCT_ID = 1L;
    private static final Long COMPANY_ID = 1L;
    private static final String BASE_URL = "/api/products/1/history";

    @BeforeEach
    void setAuth() {
        UserPrincipal principal = UserPrincipal.builder()
                .id(1L)
                .companyId(COMPANY_ID)
                .email("user@email.com")
                .username("user")
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

    private PriceHistoryResponse buildResponse(BigDecimal price, BigDecimal previousPrice) {
        return PriceHistoryResponse.builder()
                .price(price)
                .previousPrice(previousPrice)
                .changeType("INCREASE")
                .changeReason("Manual")
                .recordedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GET /api/products/{id}/history")
    class GetHistoryTests {

        @Test
        @DisplayName("Historial paginado devuelve 200")
        void getHistory_Paginated_Returns200() throws Exception {
            PriceHistoryResponse ph = buildResponse(new BigDecimal("29.99"), new BigDecimal("24.99"));
            PageResponse<PriceHistoryResponse> page = PageResponse.<PriceHistoryResponse>builder()
                    .content(List.of(ph))
                    .totalElements(1)
                    .totalPages(1)
                    .build();

            when(priceHistoryService.getHistory(eq(COMPANY_ID), eq(PRODUCT_ID), anyInt(), anyInt(), isNull(), isNull()))
                    .thenReturn(page);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].price").value(29.99))
                    .andExpect(jsonPath("$.data.content[0].changeType").value("INCREASE"));
        }

        @Test
        @DisplayName("Historial vacio devuelve pagina sin datos")
        void getHistory_Empty_ReturnsEmptyPage() throws Exception {
            PageResponse<PriceHistoryResponse> page = PageResponse.<PriceHistoryResponse>builder()
                    .content(List.of())
                    .totalElements(0)
                    .totalPages(0)
                    .build();

            when(priceHistoryService.getHistory(eq(COMPANY_ID), eq(PRODUCT_ID), anyInt(), anyInt(), isNull(), isNull()))
                    .thenReturn(page);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(0))
                    .andExpect(jsonPath("$.data.content").isEmpty());
        }

        @Test
        @DisplayName("Producto inexistente devuelve 404")
        void getHistory_ProductNotFound_Returns404() throws Exception {
            when(priceHistoryService.getHistory(eq(COMPANY_ID), eq(999L), anyInt(), anyInt(), isNull(), isNull()))
                    .thenThrow(new ResourceNotFoundException("Producto no encontrado"));

            mockMvc.perform(get("/api/products/999/history"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Historial con filtro de fechas devuelve resultados")
        void getHistory_WithDateFilter_ReturnsResults() throws Exception {
            PriceHistoryResponse ph = buildResponse(new BigDecimal("19.99"), new BigDecimal("15.99"));
            PageResponse<PriceHistoryResponse> page = PageResponse.<PriceHistoryResponse>builder()
                    .content(List.of(ph))
                    .totalElements(1)
                    .totalPages(1)
                    .build();

            when(priceHistoryService.getHistory(eq(COMPANY_ID), eq(PRODUCT_ID), anyInt(), anyInt(), any(), any()))
                    .thenReturn(page);

            mockMvc.perform(get(BASE_URL)
                            .param("startDate", "2026-01-01T00:00:00")
                            .param("endDate", "2026-02-22T23:59:59"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].price").value(19.99));
        }
    }

    @Nested
    @DisplayName("GET /api/products/{id}/history/recent")
    class GetRecentHistoryTests {

        @Test
        @DisplayName("Ultimos 10 cambios devuelve lista")
        void getRecentHistory_ReturnsList() throws Exception {
            PriceHistoryResponse ph1 = buildResponse(new BigDecimal("29.99"), new BigDecimal("24.99"));
            PriceHistoryResponse ph2 = buildResponse(new BigDecimal("24.99"), new BigDecimal("19.99"));
            when(priceHistoryService.getRecentHistory(eq(COMPANY_ID), eq(PRODUCT_ID)))
                    .thenReturn(List.of(ph1, ph2));

            mockMvc.perform(get(BASE_URL + "/recent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].price").value(29.99))
                    .andExpect(jsonPath("$.data[1].price").value(24.99));
        }

        @Test
        @DisplayName("Sin historial devuelve lista vacia")
        void getRecentHistory_Empty_ReturnsEmptyList() throws Exception {
            when(priceHistoryService.getRecentHistory(eq(COMPANY_ID), eq(PRODUCT_ID)))
                    .thenReturn(List.of());

            mockMvc.perform(get(BASE_URL + "/recent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }
}
