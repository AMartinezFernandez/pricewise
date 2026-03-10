package com.alvaro.pricewise.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alvaro.pricewise.entity.Company;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    boolean existsByTaxId(String taxId);

    Optional<Company> findByCompanyCode(String companyCode);

    boolean existsByCompanyCode(String companyCode);

    long countByActive(boolean active);

    // Cargar empresas con usuarios en una sola query (evita N+1 en admin dashboard)
    @Query("SELECT DISTINCT c FROM Company c LEFT JOIN FETCH c.users")
    List<Company> findAllWithUsers();

    // Cargar empresa con usuarios (para detalle admin)
    @Query("SELECT c FROM Company c LEFT JOIN FETCH c.users WHERE c.id = :companyId")
    Optional<Company> findByIdWithUsers(@Param("companyId") Long companyId);
}
