package com.alvaro.pricewise.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alvaro.pricewise.entity.PriceRecommendation;
import com.alvaro.pricewise.entity.PriceRecommendation.Status;

@Repository
public interface PriceRecommendationRepository extends JpaRepository<PriceRecommendation, Long> {

    // Queries ahora basadas en company (a través de product.company.id)
    Page<PriceRecommendation> findByProductCompanyId(Long companyId, Pageable pageable);

    @Query("SELECT r FROM PriceRecommendation r JOIN FETCH r.product WHERE r.product.company.id = :companyId AND r.status = :status")
    Page<PriceRecommendation> findByProductCompanyIdAndStatus(Long companyId, Status status, Pageable pageable);


    List<PriceRecommendation> findByProductIdAndStatus(Long productId, Status status);

    long countByProductCompanyIdAndStatus(Long companyId, Status status);

    @Query("SELECT r FROM PriceRecommendation r WHERE r.product.company.id = :companyId " +
           "AND r.status = :status ORDER BY r.priority DESC, r.createdAt DESC")
    List<PriceRecommendation> findTopByCompanyIdAndStatus(
            @Param("companyId") Long companyId,
            @Param("status") Status status,
            Pageable pageable);

    @Query("SELECT r.recommendationType, COUNT(r) FROM PriceRecommendation r " +
           "WHERE r.product.company.id = :companyId AND r.status = 'PENDING' " +
           "GROUP BY r.recommendationType")
    List<Object[]> countByTypeForCompany(@Param("companyId") Long companyId);

    @Query("SELECT SUM(r.potentialSavingOrProfit) FROM PriceRecommendation r " +
           "WHERE r.product.company.id = :companyId AND r.status = 'PENDING' " +
           "AND r.potentialSavingOrProfit > 0")
    java.math.BigDecimal sumPotentialSavingsForCompany(@Param("companyId") Long companyId);
}
