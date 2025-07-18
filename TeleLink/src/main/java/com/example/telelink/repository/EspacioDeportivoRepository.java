package com.example.telelink.repository;

import com.example.telelink.entity.EspacioDeportivo;
import com.example.telelink.entity.ServicioDeportivo;
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

    List<EspacioDeportivo> findTop3ByEstadoServicioOrderByEspacioDeportivoIdAsc(EspacioDeportivo.EstadoServicio estadoServicio);

    @Query("SELECT e, COALESCE(AVG(r.calificacion), 0.0) as avgRating, COALESCE(COUNT(r), 0) as reviewCount " +
            "FROM EspacioDeportivo e " +
            "LEFT JOIN Resenia r ON e.espacioDeportivoId = r.espacioDeportivo.espacioDeportivoId " +
            "WHERE e.estadoServicio = :estadoServicio " +
            "GROUP BY e " +
            "ORDER BY avgRating DESC, e.espacioDeportivoId ASC " +
            "FETCH FIRST 3 ROWS ONLY")
    List<Object[]> findTop3ByEstadoServicioOrderByAverageRatingDesc(EspacioDeportivo.EstadoServicio estadoServicio);

    List<EspacioDeportivo> findByServicioDeportivo(ServicioDeportivo servicio);

    EspacioDeportivo findByNombre(String espacioNombre);

    List<EspacioDeportivo> findByNombreContainingIgnoreCase(String espacioNombre);

}