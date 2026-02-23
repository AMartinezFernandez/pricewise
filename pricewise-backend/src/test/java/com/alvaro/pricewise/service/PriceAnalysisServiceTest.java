package com.alvaro.pricewise.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import com.alvaro.pricewise.entity.Alert;
import com.alvaro.pricewise.entity.Company;
import com.alvaro.pricewise.entity.CompetitorPrice;
import com.alvaro.pricewise.entity.PriceRecommendation;
import com.alvaro.pricewise.entity.Product;
import com.alvaro.pricewise.entity.User;
import com.alvaro.pricewise.exception.ResourceNotFoundException;
import com.alvaro.pricewise.repository.AlertRepository;
import com.alvaro.pricewise.repository.AlertRuleRepository;
import com.alvaro.pricewise.repository.CompetitorPriceRepository;
import com.alvaro.pricewise.repository.PriceRecommendationRepository;
import com.alvaro.pricewise.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("PriceAnalysisService Tests")
class PriceAnalysisServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CompetitorPriceRepository competitorPriceRepository;

    @Mock
    private PriceRecommendationRepository recommendationRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlertRuleRepository alertRuleRepository;

    private PriceAnalysisService priceAnalysisService;

    @BeforeEach
    void setUp() {
        priceAnalysisService = new PriceAnalysisService(
                productRepository,
                competitorPriceRepository,
                recommendationRepository,
                alertRepository,
                alertRuleRepository
        );
    }

    private Product createTestProduct(Long id, BigDecimal currentPrice) {
        Company company = Company.builder()
                .id(1L)
                .build();
        User user = User.builder()
                .id(1L)
                .build();
        return Product.builder()
                .id(id)
                .name("Test Product")
                .currentPrice(currentPrice)
                .company(company)
                .active(true)
                .sku("SKU-001")
                .monitoringEnabled(true)
                .createdBy(user)
                .build();
    }

    private CompetitorPrice createTestCompetitorPrice(Product product, BigDecimal price) {
        return CompetitorPrice.builder()
                .id(1L)
                .product(product)
                .price(price)
                .competitorProductTitle("Competitor Product")
                .scrapedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("analyzeProduct")
    class AnalyzeProductTests {

        @Test
        @DisplayName("Should return without creating anything when no competitor price exists")
        void analyzeProduct_noCompetitorPrice_shouldNotCreateAnything() {
            Product product = createTestProduct(1L, new BigDecimal("100.00"));

            when(competitorPriceRepository.findTopByProductIdOrderByScrapedAtDesc(1L))
                    .thenReturn(Optional.empty());

            priceAnalysisService.analyzeProduct(product);

            verify(recommendationRepository, never()).save(any(PriceRecommendation.class));
            verify(alertRepository, never()).save(any(Alert.class));
        }

        @Test
        @DisplayName("Should create PRICE_TOO_HIGH recommendation when our price is higher than competitor by more than 10%")
        void analyzeProduct_ourPriceHigher_shouldCreatePriceTooHighRecommendation() {
            Product product = createTestProduct(1L, new BigDecimal("150.00"));
            CompetitorPrice competitorPrice = createTestCompetitorPrice(product, new BigDecimal("100.00"));

            when(competitorPriceRepository.findTopByProductIdOrderByScrapedAtDesc(1L))
                    .thenReturn(Optional.of(competitorPrice));
            when(recommendationRepository.findByProductIdAndStatus(eq(1L), eq(PriceRecommendation.Status.PENDING)))
                    .thenReturn(List.of());
            when(competitorPriceRepository.findByProductIdOrderByScrapedAtDesc(eq(1L), any()))
                    .thenReturn(Page.empty());

            priceAnalysisService.analyzeProduct(product);

            ArgumentCaptor<PriceRecommendation> captor = ArgumentCaptor.forClass(PriceRecommendation.class);
            verify(recommendationRepository).save(captor.capture());

            PriceRecommendation saved = captor.getValue();
            assertThat(saved.getRecommendationType()).isEqualTo(PriceRecommendation.RecommendationType.PRICE_TOO_HIGH);
            assertThat(saved.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(saved.getCompetitorPrice()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(saved.getStatus()).isEqualTo(PriceRecommendation.Status.PENDING);
            assertThat(saved.getProduct()).isEqualTo(product);
        }

        @Test
        @DisplayName("Should create PRICE_TOO_LOW recommendation when our price is lower than competitor by more than 10%")
        void analyzeProduct_ourPriceLower_shouldCreatePriceTooLowRecommendation() {
            Product product = createTestProduct(1L, new BigDecimal("80.00"));
            CompetitorPrice competitorPrice = createTestCompetitorPrice(product, new BigDecimal("120.00"));

            when(competitorPriceRepository.findTopByProductIdOrderByScrapedAtDesc(1L))
                    .thenReturn(Optional.of(competitorPrice));
            when(recommendationRepository.findByProductIdAndStatus(eq(1L), eq(PriceRecommendation.Status.PENDING)))
                    .thenReturn(List.of());
            when(competitorPriceRepository.findByProductIdOrderByScrapedAtDesc(eq(1L), any()))
                    .thenReturn(Page.empty());

            priceAnalysisService.analyzeProduct(product);

            ArgumentCaptor<PriceRecommendation> captor = ArgumentCaptor.forClass(PriceRecommendation.class);
            verify(recommendationRepository).save(captor.capture());

            PriceRecommendation saved = captor.getValue();
            assertThat(saved.getRecommendationType()).isEqualTo(PriceRecommendation.RecommendationType.PRICE_TOO_LOW);
            assertThat(saved.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("80.00"));
            assertThat(saved.getCompetitorPrice()).isEqualByComparingTo(new BigDecimal("120.00"));
            assertThat(saved.getStatus()).isEqualTo(PriceRecommendation.Status.PENDING);
            assertThat(saved.getProduct()).isEqualTo(product);
        }

        @Test
        @DisplayName("Should not create recommendation when ourPrice is null")
        void analyzeProduct_nullOurPrice_shouldNotCreateAnything() {
            Product product = createTestProduct(1L, null);
            CompetitorPrice competitorPrice = createTestCompetitorPrice(product, new BigDecimal("100.00"));

            when(competitorPriceRepository.findTopByProductIdOrderByScrapedAtDesc(1L))
                    .thenReturn(Optional.of(competitorPrice));

            priceAnalysisService.analyzeProduct(product);

            verify(recommendationRepository, never()).save(any(PriceRecommendation.class));
            verify(alertRepository, never()).save(any(Alert.class));
        }
    }

    @Nested
    @DisplayName("analyzeAllProductsForUser")
    class AnalyzeAllProductsForUserTests {

        @Test
        @DisplayName("Should return count of analyzed products")
        void analyzeAllProductsForUser_shouldReturnCountOfAnalyzedProducts() {
            Long companyId = 1L;
            Product product1 = createTestProduct(1L, new BigDecimal("100.00"));
            Product product2 = createTestProduct(2L, new BigDecimal("200.00"));
            List<Product> products = List.of(product1, product2);

            when(productRepository.findByCompanyIdAndActiveTrue(companyId))
                    .thenReturn(products);
            when(competitorPriceRepository.findTopByProductIdOrderByScrapedAtDesc(any()))
                    .thenReturn(Optional.empty());

            int count = priceAnalysisService.analyzeAllProductsForUser(companyId);

            assertThat(count).isEqualTo(2);
            verify(productRepository).findByCompanyIdAndActiveTrue(companyId);
        }
    }

    @Nested
    @DisplayName("applyRecommendation")
    class ApplyRecommendationTests {

        @Test
        @DisplayName("Should apply recommendation successfully")
        void applyRecommendation_existingRecommendation_shouldApplySuccessfully() {
            Long recommendationId = 1L;
            Product product = createTestProduct(1L, new BigDecimal("150.00"));

            PriceRecommendation recommendation = PriceRecommendation.builder()
                    .id(recommendationId)
                    .product(product)
                    .recommendationType(PriceRecommendation.RecommendationType.PRICE_TOO_HIGH)
                    .currentPrice(new BigDecimal("150.00"))
                    .competitorPrice(new BigDecimal("100.00"))
                    .suggestedPrice(new BigDecimal("105.00"))
                    .status(PriceRecommendation.Status.PENDING)
                    .build();

            when(recommendationRepository.findById(recommendationId))
                    .thenReturn(Optional.of(recommendation));

            priceAnalysisService.applyRecommendation(1L, recommendationId);

            assertThat(recommendation.getStatus()).isEqualTo(PriceRecommendation.Status.APPLIED);
            assertThat(recommendation.getAppliedAt()).isNotNull();
            verify(recommendationRepository).save(recommendation);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when recommendation does not exist")
        void applyRecommendation_nonExistentRecommendation_shouldThrowException() {
            Long recommendationId = 999L;

            when(recommendationRepository.findById(recommendationId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> priceAnalysisService.applyRecommendation(1L, recommendationId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("dismissRecommendation")
    class DismissRecommendationTests {

        @Test
        @DisplayName("Should dismiss recommendation successfully")
        void dismissRecommendation_existingRecommendation_shouldDismissSuccessfully() {
            Long recommendationId = 1L;
            Product product = createTestProduct(1L, new BigDecimal("150.00"));

            PriceRecommendation recommendation = PriceRecommendation.builder()
                    .id(recommendationId)
                    .product(product)
                    .recommendationType(PriceRecommendation.RecommendationType.PRICE_TOO_HIGH)
                    .currentPrice(new BigDecimal("150.00"))
                    .competitorPrice(new BigDecimal("100.00"))
                    .suggestedPrice(new BigDecimal("105.00"))
                    .status(PriceRecommendation.Status.PENDING)
                    .build();

            when(recommendationRepository.findById(recommendationId))
                    .thenReturn(Optional.of(recommendation));

            priceAnalysisService.dismissRecommendation(1L, recommendationId);

            assertThat(recommendation.getStatus()).isEqualTo(PriceRecommendation.Status.DISMISSED);
            assertThat(recommendation.getDismissedAt()).isNotNull();
            verify(recommendationRepository).save(recommendation);
        }
    }

    @Nested
    @DisplayName("markAlertAsRead")
    class MarkAlertAsReadTests {

        @Test
        @DisplayName("Should mark alert as read successfully")
        void markAlertAsRead_existingAlert_shouldMarkAsRead() {
            Long alertId = 1L;
            User user = User.builder().id(1L).build();
            Product product = createTestProduct(1L, new BigDecimal("100.00"));

            Alert alert = Alert.builder()
                    .id(alertId)
                    .user(user)
                    .product(product)
                    .alertType(Alert.AlertType.COMPETITOR_PRICE_DROP)
                    .title("Price Drop Alert")
                    .message("Competitor dropped price")
                    .previousPrice(new BigDecimal("120.00"))
                    .newPrice(new BigDecimal("90.00"))
                    .severity(Alert.Severity.WARNING)
                    .isRead(false)
                    .build();

            when(alertRepository.findById(alertId))
                    .thenReturn(Optional.of(alert));

            priceAnalysisService.markAlertAsRead(1L, alertId);

            assertThat(alert.getIsRead()).isTrue();
            assertThat(alert.getReadAt()).isNotNull();
            verify(alertRepository).save(alert);
        }
    }

    @Nested
    @DisplayName("getTotalPotentialSavings")
    class GetTotalPotentialSavingsTests {

        @Test
        @DisplayName("Should return zero when repository returns null")
        void getTotalPotentialSavings_nullFromRepository_shouldReturnZero() {
            Long companyId = 1L;

            when(recommendationRepository.sumPotentialSavingsForCompany(companyId))
                    .thenReturn(null);

            BigDecimal result = priceAnalysisService.getTotalPotentialSavings(companyId);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
