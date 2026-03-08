package com.alvaro.pricewise.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.alvaro.pricewise.entity.CompanyApiKey;

public interface CompanyApiKeyRepository extends JpaRepository<CompanyApiKey, Long> {

    List<CompanyApiKey> findByCompanyIdOrderByProviderAsc(Long companyId);

    Optional<CompanyApiKey> findByCompanyIdAndProvider(Long companyId, CompanyApiKey.Provider provider);

    Optional<CompanyApiKey> findByCompanyIdAndProviderAndEnabledTrue(Long companyId, CompanyApiKey.Provider provider);

    boolean existsByCompanyIdAndProvider(Long companyId, CompanyApiKey.Provider provider);
}
