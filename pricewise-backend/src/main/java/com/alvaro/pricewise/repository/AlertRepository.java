package com.alvaro.pricewise.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alvaro.pricewise.entity.Alert;
import com.alvaro.pricewise.entity.Alert.AlertType;
import com.alvaro.pricewise.entity.Alert.Severity;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    Page<Alert> findByUserId(Long userId, Pageable pageable);

    Page<Alert> findByUserIdAndIsReadFalse(Long userId, Pageable pageable);

    Page<Alert> findByUserIdAndAlertType(Long userId, AlertType alertType, Pageable pageable);

    Page<Alert> findByUserIdAndSeverity(Long userId, Severity severity, Pageable pageable);

    List<Alert> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    long countByUserIdAndIsReadFalse(Long userId);

    long countByUserIdAndSeverity(Long userId, Severity severity);

    // Queries por empresa (multi-tenancy correcto)
    @Query("SELECT a FROM Alert a WHERE a.product.company.id = :companyId")
    Page<Alert> findByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    @Query("SELECT a FROM Alert a WHERE a.product.company.id = :companyId AND a.isRead = false")
    Page<Alert> findByCompanyIdAndIsReadFalse(@Param("companyId") Long companyId, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.product.company.id = :companyId AND a.isRead = false")
    long countByCompanyIdAndIsReadFalse(@Param("companyId") Long companyId);

    @Query("SELECT a.alertType, COUNT(a) FROM Alert a " +
           "WHERE a.user.id = :userId AND a.isRead = false GROUP BY a.alertType")
    List<Object[]> countUnreadByTypeForUser(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Alert a SET a.isRead = true, a.readAt = CURRENT_TIMESTAMP " +
           "WHERE a.product.company.id = :companyId AND a.isRead = false")
    int markAllAsReadByCompanyId(@Param("companyId") Long companyId);
}
