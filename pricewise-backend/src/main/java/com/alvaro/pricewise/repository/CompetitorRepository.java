package com.alvaro.pricewise.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alvaro.pricewise.entity.Competitor;

/**
 * Repositorio para la entidad Competitor
 */
@Repository
public interface CompetitorRepository extends JpaRepository<Competitor, Long> {

    /**
     * Busca un competidor por su codigo unico
     */
    Optional<Competitor> findByCode(String code);
}
