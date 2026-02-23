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

    Page<PriceHistory> findByProductId(Long productId, Pageable pageable);

    // Historial completo por producto (usado en tests de integracion)
    List<PriceHistory> findByProductIdOrderByRecordedAtDesc(Long productId);

    // Ultimos 10 cambios de precio
    List<PriceHistory> findTop10ByProductIdOrderByRecordedAtDesc(Long productId);

    // Historico en rango de fechas (usado por PriceHistoryController)
    @Query("SELECT ph FROM PriceHistory ph WHERE ph.product.id = :productId " +
           "AND ph.recordedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY ph.recordedAt ASC")
    List<PriceHistory> findByProductIdAndDateRange(
            @Param("productId") Long productId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Eliminar registros anteriores a una fecha (para limpieza TTL futura)
    void deleteByRecordedAtBefore(LocalDateTime before);
}
