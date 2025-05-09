package com.example.telelink.repository;

import com.example.telelink.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Integer> {
    @Query("SELECT COUNT(r) FROM Reserva r " +
            "WHERE EXTRACT(MONTH FROM r.fechaCreacion) = EXTRACT(MONTH FROM CURRENT_DATE) " +
            "AND EXTRACT(YEAR FROM r.fechaCreacion) = EXTRACT(YEAR FROM CURRENT_DATE)")
    int numeroReservasMes();

    @Query("SELECT COUNT(r) FROM Reserva r " +
            "WHERE EXTRACT(MONTH FROM r.fechaCreacion) = EXTRACT(MONTH FROM CURRENT_DATE) - 1 " +
            "AND EXTRACT(YEAR FROM r.fechaCreacion) = EXTRACT(YEAR FROM CURRENT_DATE)")
    long numeroReservasMesPasado();

    @Query(value = "SELECT SUM(COALESCE(e.precio_por_hora, 0) * TIMESTAMPDIFF(HOUR, r.inicio_reserva, r.fin_reserva)) " +
            "FROM reservas r " +
            "JOIN espacios_deportivos e ON r.espacio_deportivo_id = e.espacio_deportivo_id " +
            "WHERE r.fecha_creacion >= DATE_FORMAT(CURRENT_DATE - INTERVAL 1 MONTH, '%Y-%m-01') " + // Primer día del mes pasado
            "AND r.fecha_creacion < DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')", // Primer día del mes actual
            nativeQuery = true)
    Double obtenerMontoTotalDeReservasMesPasado();

    @Query(value = "SELECT SUM(COALESCE(e.precio_por_hora, 0) * TIMESTAMPDIFF(HOUR, r.inicio_reserva, r.fin_reserva)) " +
            "FROM reservas r " +
            "JOIN espacios_deportivos e ON r.espacio_deportivo_id = e.espacio_deportivo_id " +
            "WHERE r.fecha_creacion >= DATE_FORMAT(CURRENT_DATE, '%Y-%m-01') " + // Primer día del mes actual
            "AND r.fecha_creacion < DATE_FORMAT(CURRENT_DATE + INTERVAL 1 MONTH, '%Y-%m-01')", // Primer día del siguiente mes
            nativeQuery = true)
    Double obtenerMontoTotalDeReservasEsteMes();
    // Consulta SQL nativa para obtener el número total de reservas por servicio deportivo
    @Query(value = "SELECT COUNT(r.reserva_id) " +
            "FROM reservas r " +
            "JOIN espacios_deportivos e ON r.espacio_deportivo_id = e.espacio_deportivo_id " +
            "WHERE e.servicio_deportivo_id = ?1",
            nativeQuery = true)
    Integer obtenerNumeroReservasPorServicio(Integer servicioDeportivoId);



}