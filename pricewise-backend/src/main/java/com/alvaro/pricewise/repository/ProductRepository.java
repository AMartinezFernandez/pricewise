package com.alvaro.pricewise.repository;

import com.alvaro.pricewise.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // Buscar productos por usuario
    Page<Product> findByUserId(Long userId, Pageable pageable);

    // Solo productos activos (excluye los eliminados con soft delete)
    Page<Product> findByUserIdAndActiveTrue(Long userId, Pageable pageable);

    List<Product> findByUserIdAndActiveTrue(Long userId);
    
    // Buscar por SKU
    Optional<Product> findBySku(String sku);
    
    Optional<Product> findBySkuAndUserId(String sku, Long userId);
    
    // Buscar por EAN
    Optional<Product> findByEan(String ean);
    
    // Búsqueda por nombre (case insensitive)
    Page<Product> findByUserIdAndNameContainingIgnoreCase(Long userId, String name, Pageable pageable);
    
    // Buscar por categoría
    Page<Product> findByUserIdAndCategory(Long userId, String category, Pageable pageable);
    
    // Productos con monitoreo activo
    List<Product> findByMonitoringEnabledTrueAndActiveTrue();
    
    // Productos con monitoreo activo (paginado)
    Page<Product> findByMonitoringEnabledTrueAndActiveTrue(Pageable pageable);
    
    // Búsqueda avanzada con filtros
    @Query("SELECT p FROM Product p WHERE p.user.id = :userId " +
           "AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
           "AND (:category IS NULL OR p.category = :category) " +
           "AND (:brand IS NULL OR p.brand = :brand) " +
           "AND p.active = true")
    Page<Product> searchProducts(
            @Param("userId") Long userId,
            @Param("name") String name,
            @Param("category") String category,
            @Param("brand") String brand,
            Pageable pageable
    );
    
    // Contar productos por usuario (solo activos)
    long countByUserId(Long userId);
    long countByUserIdAndActiveTrue(Long userId);
    
    // Obtener categorías únicas del usuario
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.user.id = :userId AND p.category IS NOT NULL")
    List<String> findDistinctCategoriesByUserId(@Param("userId") Long userId);
    
    // Obtener marcas únicas del usuario
    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.user.id = :userId AND p.brand IS NOT NULL")
    List<String> findDistinctBrandsByUserId(@Param("userId") Long userId);
}
