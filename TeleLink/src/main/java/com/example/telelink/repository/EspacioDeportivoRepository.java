package com.example.telelink.repository;

import com.example.telelink.entity.EspacioDeportivo;
import com.example.telelink.entity.EstablecimientoDeportivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EspacioDeportivoRepository extends JpaRepository<EspacioDeportivo, Integer> {
    @Query("SELECT e FROM EspacioDeportivo e WHERE e.establecimientoDeportivo.establecimientoDeportivoId = :establecimientoId AND e.servicioDeportivo.servicioDeportivoId = :servicioId")
    List<EspacioDeportivo> findByEstablecimientoAndServicio(
            @Param("establecimientoId") Integer establecimientoId,
            @Param("servicioId") Integer servicioId);

    List<EspacioDeportivo> findAllByEstablecimientoDeportivo(EstablecimientoDeportivo establecimientoDeportivo);

}