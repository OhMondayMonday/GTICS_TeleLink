package com.example.telelink.repository;

import com.example.telelink.entity.EspacioDeportivo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query(value = "SELECT ed.* FROM espacios_deportivos ed " +
            "WHERE (:servicioId IS NULL OR ed.servicio_deportivo_id = :servicioId) " +
            "AND (:establecimientoId IS NULL OR ed.establecimiento_deportivo_id = :establecimientoId) " +
            "AND (:estado IS NULL OR ed.estado_servicio = :estado) " +
            "AND (:precioMinimo IS NULL OR ed.precio_por_hora >= :precioMinimo) " +
            "AND (:precioMaximo IS NULL OR ed.precio_por_hora <= :precioMaximo) " +
            "AND (:carrilesPiscina IS NULL OR ed.carriles_piscina >= :carrilesPiscina) " +
            "AND (:aforoGimnasio IS NULL OR ed.aforo_gimnasio >= :aforoGimnasio) " +
            "AND (:carrilesAtletismo IS NULL OR ed.carriles_pista >= :carrilesAtletismo) " +
            "ORDER BY ed.nombre ASC", nativeQuery = true)
    List<EspacioDeportivo> buscarConFiltros(
            @Param("servicioId") Integer servicioId,
            @Param("establecimientoId") Integer establecimientoId,
            @Param("estado") String estado,
            @Param("precioMinimo") Double precioMinimo,
            @Param("precioMaximo") Double precioMaximo,
            @Param("carrilesPiscina") Integer carrilesPiscina,
            @Param("aforoGimnasio") Integer aforoGimnasio,
            @Param("carrilesAtletismo") Integer carrilesAtletismo);
  
    Page<EspacioDeportivo> findAll(Pageable pageable);
  
    List<EspacioDeportivo> findAllByEstablecimientoDeportivo(EstablecimientoDeportivo establecimientoDeportivo);

}