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

    // Buscar producto con createdBy precargado (evita N+1 en detalle)
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.createdBy WHERE p.company.id = :companyId AND p.id = :productId AND p.active = true")
    Optional<Product> findByCompanyIdAndIdWithCreatedBy(@Param("companyId") Long companyId, @Param("productId") Long productId);

    // Solo productos activos
    Page<Product> findByCompanyIdAndActiveTrue(Long companyId, Pageable pageable);

    List<Product> findByCompanyIdAndActiveTrue(Long companyId);

    // Buscar por SKU + empresa solo entre activos (para validar duplicados ignorando soft-deleted)
    Optional<Product> findBySkuAndCompanyIdAndActiveTrue(String sku, Long companyId);

    // Productos con monitoreo activo (paginado, para PriceMonitorJob)
    Page<Product> findByMonitoringEnabledTrueAndActiveTrue(Pageable pageable);

    // Busqueda avanzada con filtros
    @Query("SELECT p FROM Product p WHERE p.company.id = :companyId " +
           "AND (:name IS NULL OR " +
           "     LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "     LOWER(p.sku) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "     LOWER(p.ean) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "     LOWER(p.asin) LIKE LOWER(CONCAT('%', :name, '%'))) " +
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

    // Contar productos por empresa
    long countByCompanyId(Long companyId);
    long countByCompanyIdAndActiveTrue(Long companyId);

    // Obtener categorias unicas de la empresa
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.company.id = :companyId AND p.category IS NOT NULL")
    List<String> findDistinctCategoriesByCompanyId(@Param("companyId") Long companyId);

    // Obtener marcas unicas de la empresa
    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.company.id = :companyId AND p.brand IS NOT NULL")
    List<String> findDistinctBrandsByCompanyId(@Param("companyId") Long companyId);
}
