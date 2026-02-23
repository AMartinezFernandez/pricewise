package com.alvaro.pricewise.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alvaro.pricewise.entity.AlertRule;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    List<AlertRule> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    Optional<AlertRule> findByIdAndCompanyId(Long id, Long companyId);

    List<AlertRule> findByCompanyIdAndEnabledTrue(Long companyId);

    @Query("SELECT r FROM AlertRule r WHERE r.company.id = :companyId AND r.enabled = true " +
           "AND (r.product IS NULL OR r.product.id = :productId) " +
           "ORDER BY r.product.id ASC NULLS FIRST")
    List<AlertRule> findApplicableRules(@Param("companyId") Long companyId,
                                        @Param("productId") Long productId);
}
