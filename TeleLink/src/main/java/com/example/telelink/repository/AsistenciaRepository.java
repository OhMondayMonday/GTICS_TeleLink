package com.example.telelink.repository;

import com.example.telelink.entity.Asistencia;
import com.example.telelink.entity.Mantenimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, Integer> {

    @Query("SELECT a FROM Asistencia a " +
            "JOIN FETCH a.coordinador " +
            "JOIN FETCH a.espacioDeportivo ed " +
            "JOIN FETCH ed.servicioDeportivo " +
            "JOIN FETCH ed.establecimientoDeportivo " +
            "WHERE a.coordinador.usuarioId = :userId " +
            "AND a.horarioEntrada BETWEEN :start AND :end")
    List<Asistencia> findForCalendarRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("userId") int userId);

    @Query("SELECT a.estadoEntrada AS estado, COUNT(a) AS cantidad " +
            "FROM Asistencia a " +
            "WHERE a.coordinador.usuarioId = :userId " +
            "AND a.estadoEntrada IN ('puntual', 'tarde', 'inasistencia') " +
            "GROUP BY a.estadoEntrada")
    List<Object[]> countByEstadoEntradaRaw(@Param("userId") int userId);

    default Map<String, Long> countByEstadoEntrada(int userId) {
        List<Object[]> results = countByEstadoEntradaRaw(userId);
        Map<String, Long> estadisticas = new HashMap<>();
        estadisticas.put("puntual", 0L);
        estadisticas.put("tarde", 0L);
        estadisticas.put("inasistencia", 0L);
        for (Object[] result : results) {
            String estado = ((Asistencia.EstadoEntrada) result[0]).name();
            Long cantidad = (Long) result[1];
            estadisticas.put(estado, cantidad);
        }
        return estadisticas;
    }

    List<Asistencia> findByCoordinador_UsuarioId(Integer usuarioId);

    @Query("SELECT a FROM Asistencia a " +
            "WHERE a.coordinador.usuarioId = :coordinadorId " +
            "AND a.horarioEntrada <= :endCheck " +
            "AND a.horarioSalida >= :startCheck")
  
    List<Asistencia> findOverlappingAsistencias(
            @Param("coordinadorId") int coordinadorId,
            @Param("startCheck") LocalDateTime startCheck,
            @Param("endCheck") LocalDateTime endCheck);    @Query("SELECT a FROM Asistencia a " +
           "JOIN FETCH a.coordinador " +
           "WHERE a.espacioDeportivo.espacioDeportivoId = :espacioId " +
           "AND a.horarioEntrada < :fin " +
           "AND a.horarioSalida > :inicio")
    List<Asistencia> findAsistenciasEnRango(
            @Param("espacioId") Integer espacioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    @Query("SELECT a FROM Asistencia a " +
            "WHERE a.coordinador.usuarioId = :coordinadorId " +
            "AND a.horarioEntrada < :finRango " +
            "AND a.horarioSalida > :inicioRango")
    List<Asistencia> findAsistenciasSuperpuestas(
            @Param("coordinadorId") Integer coordinadorId,
            @Param("inicioRango") LocalDateTime inicioRango,
            @Param("finRango") LocalDateTime finRango);
}