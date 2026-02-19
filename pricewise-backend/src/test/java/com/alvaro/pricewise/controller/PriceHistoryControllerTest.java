package com.alvaro.pricewise.controller;

import com.alvaro.pricewise.entity.PriceHistory;
import com.alvaro.pricewise.entity.Product;
import com.alvaro.pricewise.exception.GlobalExceptionHandler;
import com.alvaro.pricewise.exception.ResourceNotFoundException;
import com.alvaro.pricewise.repository.PriceHistoryRepository;
import com.alvaro.pricewise.repository.ProductRepository;
import com.alvaro.pricewise.security.JwtService;
import com.alvaro.pricewise.security.UserPrincipal;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private PriceHistoryRepository priceHistoryRepository;

    @MockBean
    private ProductRepository productRepository;

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

        // Producto existe y pertenece a la empresa
        Product product = Product.builder().id(PRODUCT_ID).name("Test Product").build();
        when(productRepository.findByCompanyIdAndIdWithCreatedBy(eq(COMPANY_ID), eq(PRODUCT_ID)))
                .thenReturn(Optional.of(product));
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private PriceHistory buildPriceHistory(Long id, BigDecimal price, BigDecimal previousPrice) {
        return PriceHistory.builder()
                .id(id)
                .price(price)
                .previousPrice(previousPrice)
                .changeType(PriceHistory.ChangeType.INCREASE)
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
            PriceHistory ph = buildPriceHistory(1L, new BigDecimal("29.99"), new BigDecimal("24.99"));
            when(priceHistoryRepository.findByProductId(eq(PRODUCT_ID), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(ph)));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].price").value(29.99))
                    .andExpect(jsonPath("$.data.content[0].changeType").value("INCREASE"));
        }

        @Test
        @DisplayName("Historial vacio devuelve pagina sin datos")
        void getHistory_Empty_ReturnsEmptyPage() throws Exception {
            when(priceHistoryRepository.findByProductId(eq(PRODUCT_ID), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(0))
                    .andExpect(jsonPath("$.data.content").isEmpty());
        }

        @Test
        @DisplayName("Producto inexistente devuelve 404")
        void getHistory_ProductNotFound_Returns404() throws Exception {
            when(productRepository.findByCompanyIdAndIdWithCreatedBy(eq(COMPANY_ID), eq(999L)))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/products/999/history"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Historial con filtro de fechas devuelve resultados")
        void getHistory_WithDateFilter_ReturnsResults() throws Exception {
            PriceHistory ph = buildPriceHistory(1L, new BigDecimal("19.99"), new BigDecimal("15.99"));
            when(priceHistoryRepository.findByProductIdAndDateRange(eq(PRODUCT_ID), any(), any()))
                    .thenReturn(List.of(ph));

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
            PriceHistory ph1 = buildPriceHistory(1L, new BigDecimal("29.99"), new BigDecimal("24.99"));
            PriceHistory ph2 = buildPriceHistory(2L, new BigDecimal("24.99"), new BigDecimal("19.99"));
            when(priceHistoryRepository.findTop10ByProductIdOrderByRecordedAtDesc(eq(PRODUCT_ID)))
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
            when(priceHistoryRepository.findTop10ByProductIdOrderByRecordedAtDesc(eq(PRODUCT_ID)))
                    .thenReturn(List.of());

            mockMvc.perform(get(BASE_URL + "/recent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }
}
