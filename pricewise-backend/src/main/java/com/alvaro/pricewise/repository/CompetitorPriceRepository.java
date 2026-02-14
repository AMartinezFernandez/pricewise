package com.alvaro.pricewise.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alvaro.pricewise.entity.Competitor;
import com.alvaro.pricewise.entity.CompetitorPrice;
import com.alvaro.pricewise.entity.Product;

/**
 * Repositorio para la entidad CompetitorPrice
 */
@Repository
public interface CompetitorPriceRepository extends JpaRepository<CompetitorPrice, Long> {

    /**
     * Obtiene los precios más recientes de un producto en todos los competidores
     */
    @Query("SELECT cp FROM CompetitorPrice cp WHERE cp.product = :product " +
           "AND cp.scrapedAt = (SELECT MAX(cp2.scrapedAt) FROM CompetitorPrice cp2 " +
           "WHERE cp2.product = cp.product AND cp2.competitor = cp.competitor)")
    List<CompetitorPrice> findLatestPricesByProduct(@Param("product") Product product);

    /**
     * Obtiene el precio más reciente de un producto en un competidor específico
     */
    Optional<CompetitorPrice> findFirstByProductAndCompetitorOrderByScrapedAtDesc(
            Product product, Competitor competitor);

    /**
     * Lista todos los precios de un producto ordenados por fecha
     */
    List<CompetitorPrice> findByProductOrderByScrapedAtDesc(Product product);

    /**
     * Lista precios de un producto en un rango de fechas
     */
    List<CompetitorPrice> findByProductAndScrapedAtBetweenOrderByScrapedAtAsc(
            Product product, LocalDateTime start, LocalDateTime end);

    /**
     * Lista precios de un competidor específico (paginado)
     */
    Page<CompetitorPrice> findByCompetitor(Competitor competitor, Pageable pageable);

    /**
     * Cuenta cuántos precios se han capturado hoy
     */
    @Query("SELECT COUNT(cp) FROM CompetitorPrice cp WHERE cp.scrapedAt >= :today")
    long countScrapedToday(@Param("today") LocalDateTime today);

    /**
     * Obtiene el precio promedio de la competencia para un producto
     */
    @Query("SELECT AVG(cp.price) FROM CompetitorPrice cp WHERE cp.product = :product " +
           "AND cp.scrapedAt >= :since AND cp.available = true")
    Optional<Double> findAveragePriceByProductSince(
            @Param("product") Product product, 
            @Param("since") LocalDateTime since);

    /**
     * Encuentra productos donde el competidor tiene mejor precio
     */
    @Query("SELECT cp FROM CompetitorPrice cp WHERE cp.product.company.id = :companyId " +
           "AND cp.price < cp.product.currentPrice " +
           "AND cp.scrapedAt >= :since " +
           "ORDER BY (cp.product.currentPrice - cp.price) DESC")
    List<CompetitorPrice> findBetterPricesThanOurs(
            @Param("companyId") Long companyId, 
            @Param("since") LocalDateTime since);

    /**
     * Elimina precios antiguos (para limpieza)
     */
    void deleteByScrapedAtBefore(LocalDateTime before);

    /**
     * Obtiene el precio mas reciente de un producto
     */
    Optional<CompetitorPrice> findTopByProductIdOrderByScrapedAtDesc(Long productId);

    /**
     * Obtiene precios de un producto paginados
     */
    Page<CompetitorPrice> findByProductIdOrderByScrapedAtDesc(Long productId, Pageable pageable);
}
