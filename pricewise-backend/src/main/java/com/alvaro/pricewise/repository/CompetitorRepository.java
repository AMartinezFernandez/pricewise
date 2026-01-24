package com.alvaro.pricewise.repository;

import java.util.List;
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
     * Busca un competidor por su código único
     */
    Optional<Competitor> findByCode(String code);

    /**
     * Busca un competidor por su nombre
     */
    Optional<Competitor> findByName(String name);

    /**
     * Lista todos los competidores activos
     */
    List<Competitor> findByActiveTrue();

    /**
     * Lista competidores activos por tipo de fuente
     */
    List<Competitor> findByActiveTrueAndSourceType(Competitor.SourceType sourceType);

    /**
     * Verifica si existe un competidor con el código dado
     */
    boolean existsByCode(String code);
}
