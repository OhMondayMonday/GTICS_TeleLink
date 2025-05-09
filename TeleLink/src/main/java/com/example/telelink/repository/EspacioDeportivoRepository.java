package com.example.telelink.repository;

import com.example.telelink.entity.EspacioDeportivo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    List<EspacioDeportivo> findByServicioDeportivoServicioDeportivoId(Integer servicioDeportivoId);

    List<EspacioDeportivo> findByEstadoServicio(EspacioDeportivo.EstadoServicio estadoServicio);

    // Mostrar solo los espacios operativos ordenados por nombre
    List<EspacioDeportivo> findByEstadoServicioOrderByNombreAsc(EspacioDeportivo.EstadoServicio estadoServicio);

    // Método para buscar canchas con filtros
    /*
    @Query("SELECT e FROM EspacioDeportivo e WHERE " +
            "(?1 IS NULL OR e.tipo = ?1) AND " +
            "(?2 IS NULL OR e.precio <= ?2) AND " +
            "(?3 IS NULL OR e.zona = ?3) AND " +
            "(?4 IS NULL OR e.rating >= ?4)")
    Page<EspacioDeportivo> findByFilters(String tipo, Double precioMax, String zona, Integer rating, Pageable pageable);
    */
    // Método para obtener todas las canchas
    Page<EspacioDeportivo> findAll(Pageable pageable);

}