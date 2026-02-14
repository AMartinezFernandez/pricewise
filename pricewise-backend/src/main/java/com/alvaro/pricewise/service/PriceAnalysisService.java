package com.alvaro.pricewise.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.pricewise.entity.Alert;
import com.alvaro.pricewise.entity.CompetitorPrice;
import com.alvaro.pricewise.entity.PriceRecommendation;
import com.alvaro.pricewise.entity.Product;
import com.alvaro.pricewise.repository.AlertRepository;
import com.alvaro.pricewise.repository.CompetitorPriceRepository;
import com.alvaro.pricewise.repository.PriceRecommendationRepository;
import com.alvaro.pricewise.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio de analisis de precios y generacion de recomendaciones.
 * Opera a nivel de empresa (companyId) para multi-tenancy.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceAnalysisService {

    private final ProductRepository productRepository;
    private final CompetitorPriceRepository competitorPriceRepository;
    private final PriceRecommendationRepository recommendationRepository;
    private final AlertRepository alertRepository;

    private static final BigDecimal HIGH_PRICE_THRESHOLD = new BigDecimal("0.10");
    private static final BigDecimal LOW_PRICE_THRESHOLD = new BigDecimal("0.10");
    private static final BigDecimal SUDDEN_CHANGE_THRESHOLD = new BigDecimal("0.15");

    @Transactional
    public void analyzeProduct(Product product) {
        Optional<CompetitorPrice> latestPriceOpt = competitorPriceRepository
                .findTopByProductIdOrderByScrapedAtDesc(product.getId());

        if (latestPriceOpt.isEmpty()) {
            return;
        }

        CompetitorPrice competitorPrice = latestPriceOpt.get();
        BigDecimal ourPrice = product.getCurrentPrice();
        BigDecimal theirPrice = competitorPrice.getPrice();

        if (ourPrice == null || theirPrice == null || theirPrice.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        BigDecimal difference = ourPrice.subtract(theirPrice);
        BigDecimal percentDiff = difference.divide(theirPrice, 4, RoundingMode.HALF_UP);

        if (percentDiff.compareTo(HIGH_PRICE_THRESHOLD) > 0) {
            createRecommendation(product, competitorPrice,
                    PriceRecommendation.RecommendationType.PRICE_TOO_HIGH,
                    calculateSuggestedPrice(theirPrice, new BigDecimal("0.02")),
                    percentDiff,
                    "Precio " + formatPercent(percentDiff) + " por encima de la competencia");
        }

        if (percentDiff.compareTo(LOW_PRICE_THRESHOLD.negate()) < 0) {
            createRecommendation(product, competitorPrice,
                    PriceRecommendation.RecommendationType.PRICE_TOO_LOW,
                    calculateSuggestedPrice(theirPrice, new BigDecimal("-0.05")),
                    percentDiff.abs(),
                    "Oportunidad: precio " + formatPercent(percentDiff.abs()) + " por debajo de competencia");
        }

        checkSuddenChange(product, competitorPrice);
    }

    /**
     * Analiza todos los productos de una empresa.
     */
    @Transactional
    public int analyzeAllProductsForUser(Long companyId) {
        List<Product> products = productRepository.findByCompanyIdAndActiveTrue(companyId);
        int analyzed = 0;

        for (Product product : products) {
            try {
                analyzeProduct(product);
                analyzed++;
            } catch (Exception e) {
                log.error("Error analizando producto {}: {}", product.getId(), e.getMessage());
            }
        }

        log.info("Analizados {} productos para empresa {}", analyzed, companyId);
        return analyzed;
    }

    private void checkSuddenChange(Product product, CompetitorPrice currentPrice) {
        Page<CompetitorPrice> previousPrices = competitorPriceRepository
                .findByProductIdOrderByScrapedAtDesc(product.getId(), PageRequest.of(1, 1));

        if (previousPrices.isEmpty()) {
            return;
        }

        CompetitorPrice previousPrice = previousPrices.getContent().get(0);
        BigDecimal current = currentPrice.getPrice();
        BigDecimal previous = previousPrice.getPrice();

        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        BigDecimal change = current.subtract(previous).divide(previous, 4, RoundingMode.HALF_UP);

        if (change.abs().compareTo(SUDDEN_CHANGE_THRESHOLD) > 0) {
            Alert.AlertType alertType = change.compareTo(BigDecimal.ZERO) < 0
                    ? Alert.AlertType.COMPETITOR_PRICE_DROP
                    : Alert.AlertType.COMPETITOR_PRICE_RISE;

            Alert.Severity severity = change.abs().compareTo(new BigDecimal("0.25")) > 0
                    ? Alert.Severity.CRITICAL
                    : Alert.Severity.WARNING;

            // Alerts still reference createdBy user for the product
            createAlert(product, alertType, severity,
                    "Cambio brusco de precio en competencia",
                    "El competidor " + (change.compareTo(BigDecimal.ZERO) < 0 ? "bajo" : "subio") +
                    " el precio " + formatPercent(change.abs()),
                    previous, current, change);
        }
    }

    private void createRecommendation(Product product, CompetitorPrice competitorPrice,
                                       PriceRecommendation.RecommendationType type,
                                       BigDecimal suggestedPrice, BigDecimal percentDiff, String reason) {

        List<PriceRecommendation> existing = recommendationRepository
                .findByProductIdAndStatus(product.getId(), PriceRecommendation.Status.PENDING);

        boolean alreadyExists = existing.stream()
                .anyMatch(r -> r.getRecommendationType() == type);

        if (alreadyExists) {
            return;
        }

        BigDecimal potentialProfit = suggestedPrice.subtract(product.getCurrentPrice());
        PriceRecommendation.Priority priority = determinePriority(percentDiff);

        PriceRecommendation recommendation = PriceRecommendation.builder()
                .product(product)
                .recommendationType(type)
                .currentPrice(product.getCurrentPrice())
                .competitorPrice(competitorPrice.getPrice())
                .suggestedPrice(suggestedPrice)
                .priceDifferencePercent(percentDiff.multiply(new BigDecimal("100")))
                .potentialSavingOrProfit(potentialProfit.abs())
                .reason(reason)
                .status(PriceRecommendation.Status.PENDING)
                .priority(priority)
                .build();

        recommendationRepository.save(recommendation);
        log.debug("Recomendacion creada: {} para producto {}", type, product.getId());
    }

    private void createAlert(Product product, Alert.AlertType type, Alert.Severity severity,
                             String title, String message, BigDecimal previousPrice,
                             BigDecimal newPrice, BigDecimal changePercent) {

        Alert alert = Alert.builder()
                .user(product.getCreatedBy()) // Use createdBy user (audit field) for alert
                .product(product)
                .alertType(type)
                .title(title)
                .message(message)
                .previousPrice(previousPrice)
                .newPrice(newPrice)
                .changePercent(changePercent.multiply(new BigDecimal("100")))
                .severity(severity)
                .isRead(false)
                .build();

        alertRepository.save(alert);
        log.debug("Alerta creada: {} para producto {}", type, product.getId());
    }

    private BigDecimal calculateSuggestedPrice(BigDecimal competitorPrice, BigDecimal marginPercent) {
        return competitorPrice.multiply(BigDecimal.ONE.add(marginPercent))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private PriceRecommendation.Priority determinePriority(BigDecimal percentDiff) {
        BigDecimal abs = percentDiff.abs();
        if (abs.compareTo(new BigDecimal("0.30")) > 0) {
            return PriceRecommendation.Priority.URGENT;
        } else if (abs.compareTo(new BigDecimal("0.20")) > 0) {
            return PriceRecommendation.Priority.HIGH;
        } else if (abs.compareTo(new BigDecimal("0.10")) > 0) {
            return PriceRecommendation.Priority.MEDIUM;
        }
        return PriceRecommendation.Priority.LOW;
    }

    private String formatPercent(BigDecimal percent) {
        return percent.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP) + "%";
    }

    // Metodos de consulta — ahora basados en companyId

    @Transactional(readOnly = true)
    public Page<PriceRecommendation> getPendingRecommendations(Long companyId, Pageable pageable) {
        return recommendationRepository.findByProductCompanyIdAndStatus(
                companyId, PriceRecommendation.Status.PENDING, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Alert> getUnreadAlerts(Long userId, Pageable pageable) {
        return alertRepository.findByUserIdAndIsReadFalse(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long countPendingRecommendations(Long companyId) {
        return recommendationRepository.countByProductCompanyIdAndStatus(
                companyId, PriceRecommendation.Status.PENDING);
    }

    @Transactional(readOnly = true)
    public long countUnreadAlerts(Long userId) {
        return alertRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalPotentialSavings(Long companyId) {
        BigDecimal savings = recommendationRepository.sumPotentialSavingsForCompany(companyId);
        return savings != null ? savings : BigDecimal.ZERO;
    }

    @Transactional
    public void applyRecommendation(Long recommendationId) {
        PriceRecommendation rec = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new IllegalArgumentException("Recomendacion no encontrada"));

        Product product = rec.getProduct();
        product.setCurrentPrice(rec.getSuggestedPrice());
        productRepository.save(product);

        rec.setStatus(PriceRecommendation.Status.APPLIED);
        rec.setAppliedAt(LocalDateTime.now());
        recommendationRepository.save(rec);

        log.info("Recomendacion {} aplicada a producto {}", recommendationId, product.getId());
    }

    @Transactional
    public void dismissRecommendation(Long recommendationId) {
        PriceRecommendation rec = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new IllegalArgumentException("Recomendacion no encontrada"));

        rec.setStatus(PriceRecommendation.Status.DISMISSED);
        rec.setDismissedAt(LocalDateTime.now());
        recommendationRepository.save(rec);
    }

    @Transactional
    public void markAlertAsRead(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alerta no encontrada"));

        alert.setIsRead(true);
        alert.setReadAt(LocalDateTime.now());
        alertRepository.save(alert);
    }
}
