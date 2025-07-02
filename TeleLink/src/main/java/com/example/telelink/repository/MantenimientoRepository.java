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

       List<Mantenimiento> findByEspacioDeportivo_EspacioDeportivoId(Integer espacioDeportivoId);

       List<Mantenimiento> findByEstado(Mantenimiento.Estado estado);

       @Query("SELECT m FROM Mantenimiento m WHERE m.espacioDeportivo.espacioDeportivoId = :espacioId " +
                     "AND m.estado IN :estados " +
                     "AND m.fechaInicio < :fin " +
                     "AND m.fechaEstimadaFin > :inicio")
       List<Mantenimiento> findByEspacioDeportivo_EspacioDeportivoIdAndEstadoInAndFechaInicioBeforeAndFechaEstimadaFinAfter(
                     @Param("espacioId") Integer espacioId,
                     @Param("estados") List<Mantenimiento.Estado> estados,
                     @Param("fin") LocalDateTime fin,
                     @Param("inicio") LocalDateTime inicio);

       @Query("SELECT m FROM Mantenimiento m WHERE m.espacioDeportivo.espacioDeportivoId = :espacioId " +
                     "AND m.estado IN :estados " +
                     "AND m.fechaEstimadaFin > :fechaActual")
       List<Mantenimiento> findByEspacioDeportivo_EspacioDeportivoIdAndEstadoInAndFechaEstimadaFinAfter(
                     @Param("espacioId") Integer espacioId,
                     @Param("estados") List<Mantenimiento.Estado> estados,
                     @Param("fechaActual") LocalDateTime fechaActual);

       @Query("SELECT m FROM Mantenimiento m WHERE m.fechaInicio >= :fechaInicio " +
                     "AND m.fechaInicio <= :fechaFin " +
                     "AND m.estado IN :estados")
       List<Mantenimiento> findByFechaInicioRangeAndEstadoIn(
                     @Param("fechaInicio") LocalDateTime fechaInicio,
                     @Param("fechaFin") LocalDateTime fechaFin,
                     @Param("estados") List<Mantenimiento.Estado> estados);

       @Query("SELECT m FROM Mantenimiento m WHERE m.espacioDeportivo.espacioDeportivoId = :espacioId " +
                     "AND m.estado = :estado " +
                     "ORDER BY m.fechaInicio ASC")
       List<Mantenimiento> findByEspacioDeportivoIdAndEstadoOrderByFechaInicio(
                     @Param("espacioId") Integer espacioId,
                     @Param("estado") Mantenimiento.Estado estado);

       // Nuevos métodos para mantenimientos activos
       @Query("SELECT m FROM Mantenimiento m WHERE m.estado IN ('pendiente', 'en_curso') " +
                     "AND m.fechaEstimadaFin > :fechaActual")
       List<Mantenimiento> findMantenimientosActivos(@Param("fechaActual") LocalDateTime fechaActual);

       @Query("SELECT COUNT(m) FROM Mantenimiento m WHERE m.espacioDeportivo.espacioDeportivoId = :espacioId " +
                     "AND m.estado IN ('pendiente', 'en_curso') " +
                     "AND m.fechaInicio < :fin AND m.fechaEstimadaFin > :inicio")
       long countMantenimientosActivosEnHorario(
                     @Param("espacioId") Integer espacioId,
                     @Param("inicio") LocalDateTime inicio,
                     @Param("fin") LocalDateTime fin);
}