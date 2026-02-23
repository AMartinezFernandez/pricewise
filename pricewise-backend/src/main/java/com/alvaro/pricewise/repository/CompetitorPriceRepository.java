package com.alvaro.pricewise.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alvaro.pricewise.entity.CompetitorPrice;

/**
 * Repositorio para la entidad CompetitorPrice
 */
@Repository
public interface CompetitorPriceRepository extends JpaRepository<CompetitorPrice, Long> {

    /**
     * Cuenta cuantos precios se han capturado hoy
     */
    @Query("SELECT COUNT(cp) FROM CompetitorPrice cp WHERE cp.scrapedAt >= :today")
    long countScrapedToday(@Param("today") LocalDateTime today);

    /**
     * Elimina precios antiguos (para limpieza TTL futura)
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
