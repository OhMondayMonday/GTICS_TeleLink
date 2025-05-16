package com.example.telelink.repository;

import com.example.telelink.entity.Reserva;
import com.example.telelink.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Integer> {
    @Query("SELECT COUNT(r) FROM Reserva r " +
            "WHERE EXTRACT(MONTH FROM r.fechaCreacion) = EXTRACT(MONTH FROM CURRENT_DATE) " +
            "AND EXTRACT(YEAR FROM r.fechaCreacion) = EXTRACT(YEAR FROM CURRENT_DATE)")
    Integer numeroReservasMes();

    @Query("SELECT COUNT(r) FROM Reserva r " +
            "WHERE EXTRACT(MONTH FROM r.fechaCreacion) = EXTRACT(MONTH FROM CURRENT_DATE) - 1 " +
            "AND EXTRACT(YEAR FROM r.fechaCreacion) = EXTRACT(YEAR FROM CURRENT_DATE)")
    Integer numeroReservasMesPasado();

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
    
    Reserva findByReservaId(Integer reservaId);

    @Query("SELECT r FROM Reserva r JOIN FETCH r.espacioDeportivo ed  WHERE r.usuario = :usuario ORDER BY r.inicioReserva DESC LIMIT 6")
    List<Reserva> findTop6ByUsuarioOrderByInicioReservaDesc(@Param("usuario") Usuario usuario);

    long countByUsuario(Usuario usuario);

    @Query("SELECT COUNT(r) FROM Reserva r WHERE r.usuario = :usuario " +
            "AND YEAR(r.fechaCreacion) = YEAR(CURRENT_DATE) " +
            "AND MONTH(r.fechaCreacion) = MONTH(CURRENT_DATE)")
    long countByUsuarioThisMonth(@Param("usuario") Usuario usuario);

    @Query("SELECT COUNT(r) FROM Reserva r WHERE r.usuario = :usuario " +
            "AND YEAR(r.fechaCreacion) = YEAR(CURRENT_DATE) " +
            "AND WEEK(r.fechaCreacion) = WEEK(CURRENT_DATE)")
    long countByUsuarioThisWeek(@Param("usuario") Usuario usuario);

    List<Reserva> findByUsuario_UsuarioId(Integer usuarioUsuarioId);

    List<Reserva> findByUsuarioAndEstado(Usuario usuario, Reserva.Estado estado);

    List<Reserva> findByInicioReservaBetween(LocalDateTime inicio, LocalDateTime fin);

}