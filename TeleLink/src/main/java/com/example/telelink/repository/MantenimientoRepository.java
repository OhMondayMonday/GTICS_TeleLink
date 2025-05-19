package com.example.telelink.repository;

import com.example.telelink.entity.Mantenimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MantenimientoRepository extends JpaRepository<Mantenimiento, Integer> {
    @Query("SELECT m FROM Mantenimiento m " +
            "WHERE m.espacioDeportivo.espacioDeportivoId = :espacioDeportivoId " +
            "AND m.fechaInicio <= :endCheck " +
            "AND m.fechaEstimadaFin >= :startCheck " +
            "AND m.estado != 'finalizado'")
    List<Mantenimiento> findOverlappingMantenimientos(
            @Param("espacioDeportivoId") int espacioDeportivoId,
            @Param("startCheck") LocalDateTime startCheck,
            @Param("endCheck") LocalDateTime endCheck);
}