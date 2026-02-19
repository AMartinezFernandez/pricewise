package com.alvaro.pricewise.repository;

import com.alvaro.pricewise.entity.PriceHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    
    // Histórico de un producto ordenado por fecha
    List<PriceHistory> findByProductIdOrderByRecordedAtDesc(Long productId);
    
    Page<PriceHistory> findByProductId(Long productId, Pageable pageable);
    
    // Últimos N cambios de precio
    List<PriceHistory> findTop10ByProductIdOrderByRecordedAtDesc(Long productId);
    
    // Histórico en rango de fechas
    @Query("SELECT ph FROM PriceHistory ph WHERE ph.product.id = :productId " +
           "AND ph.recordedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY ph.recordedAt ASC")
    List<PriceHistory> findByProductIdAndDateRange(
            @Param("productId") Long productId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    // Último registro de precio
    PriceHistory findFirstByProductIdOrderByRecordedAtDesc(Long productId);
    
    // Contar cambios de precio en período
    @Query("SELECT COUNT(ph) FROM PriceHistory ph WHERE ph.product.id = :productId " +
           "AND ph.recordedAt >= :since")
    long countPriceChangesSince(@Param("productId") Long productId, @Param("since") LocalDateTime since);

    // Eliminar registros anteriores a una fecha (para limpieza TTL)
    void deleteByRecordedAtBefore(LocalDateTime before);
}
