package com.alvaro.pricewise.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alvaro.pricewise.entity.Alert;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    // Queries por empresa (multi-tenancy)
    @Query("SELECT a FROM Alert a WHERE a.product.company.id = :companyId")
    Page<Alert> findByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    @Query("SELECT a FROM Alert a WHERE a.product.company.id = :companyId AND a.isRead = false")
    Page<Alert> findByCompanyIdAndIsReadFalse(@Param("companyId") Long companyId, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.product.company.id = :companyId AND a.isRead = false")
    long countByCompanyIdAndIsReadFalse(@Param("companyId") Long companyId);

    @Modifying
    @Query("UPDATE Alert a SET a.isRead = true, a.readAt = CURRENT_TIMESTAMP " +
           "WHERE a.product.company.id = :companyId AND a.isRead = false")
    int markAllAsReadByCompanyId(@Param("companyId") Long companyId);

    @Modifying
    @Query("UPDATE Alert a SET a.user = NULL WHERE a.user.id = :userId")
    void nullifyUserForAlerts(@Param("userId") Long userId);
}
