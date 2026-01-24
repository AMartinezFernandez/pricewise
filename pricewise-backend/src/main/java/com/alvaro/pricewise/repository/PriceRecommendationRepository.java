package com.alvaro.pricewise.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alvaro.pricewise.entity.PriceRecommendation;
import com.alvaro.pricewise.entity.PriceRecommendation.RecommendationType;
import com.alvaro.pricewise.entity.PriceRecommendation.Status;

@Repository
public interface PriceRecommendationRepository extends JpaRepository<PriceRecommendation, Long> {

    Page<PriceRecommendation> findByProductUserId(Long userId, Pageable pageable);

    Page<PriceRecommendation> findByProductUserIdAndStatus(Long userId, Status status, Pageable pageable);

    List<PriceRecommendation> findByProductIdAndStatus(Long productId, Status status);

    long countByProductUserIdAndStatus(Long userId, Status status);

    @Query("SELECT r FROM PriceRecommendation r WHERE r.product.user.id = :userId " +
           "AND r.status = :status ORDER BY r.priority DESC, r.createdAt DESC")
    List<PriceRecommendation> findTopByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") Status status,
            Pageable pageable);

    @Query("SELECT r.recommendationType, COUNT(r) FROM PriceRecommendation r " +
           "WHERE r.product.user.id = :userId AND r.status = 'PENDING' " +
           "GROUP BY r.recommendationType")
    List<Object[]> countByTypeForUser(@Param("userId") Long userId);

    @Query("SELECT SUM(r.potentialSavingOrProfit) FROM PriceRecommendation r " +
           "WHERE r.product.user.id = :userId AND r.status = 'PENDING' " +
           "AND r.potentialSavingOrProfit > 0")
    java.math.BigDecimal sumPotentialSavingsForUser(@Param("userId") Long userId);
}
