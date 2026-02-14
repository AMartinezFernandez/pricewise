package com.alvaro.pricewise.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alvaro.pricewise.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // Buscar productos por empresa
    Page<Product> findByCompanyId(Long companyId, Pageable pageable);

    // Solo productos activos
    Page<Product> findByCompanyIdAndActiveTrue(Long companyId, Pageable pageable);

    List<Product> findByCompanyIdAndActiveTrue(Long companyId);
    
    // Buscar por SKU
    Optional<Product> findBySku(String sku);
    
    Optional<Product> findBySkuAndCompanyId(String sku, Long companyId);
    
    // Buscar por EAN
    Optional<Product> findByEan(String ean);
    
    // Búsqueda por nombre (case insensitive)
    Page<Product> findByCompanyIdAndNameContainingIgnoreCase(Long companyId, String name, Pageable pageable);
    
    // Buscar por categoría
    Page<Product> findByCompanyIdAndCategory(Long companyId, String category, Pageable pageable);
    
    // Productos con monitoreo activo
    List<Product> findByMonitoringEnabledTrueAndActiveTrue();
    
    // Productos con monitoreo activo (paginado)
    Page<Product> findByMonitoringEnabledTrueAndActiveTrue(Pageable pageable);
    
    // Búsqueda avanzada con filtros
    @Query("SELECT p FROM Product p WHERE p.company.id = :companyId " +
           "AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
           "AND (:category IS NULL OR p.category = :category) " +
           "AND (:brand IS NULL OR p.brand = :brand) " +
           "AND p.active = true")
    Page<Product> searchProducts(
            @Param("companyId") Long companyId,
            @Param("name") String name,
            @Param("category") String category,
            @Param("brand") String brand,
            Pageable pageable
    );
    
    // Contar productos por empresa (solo activos)
    long countByCompanyId(Long companyId);
    long countByCompanyIdAndActiveTrue(Long companyId);
    
    // Obtener categorías únicas de la empresa
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.company.id = :companyId AND p.category IS NOT NULL")
    List<String> findDistinctCategoriesByCompanyId(@Param("companyId") Long companyId);
    
    // Obtener marcas únicas de la empresa
    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.company.id = :companyId AND p.brand IS NOT NULL")
    List<String> findDistinctBrandsByCompanyId(@Param("companyId") Long companyId);
}

