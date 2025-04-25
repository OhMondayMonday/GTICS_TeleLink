package com.example.telelink.repository;

import com.example.telelink.entity.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

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
}