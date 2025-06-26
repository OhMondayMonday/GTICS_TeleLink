package com.example.telelink.service;

import com.example.telelink.entity.EspacioDeportivo;
import com.example.telelink.entity.Reserva;
import com.example.telelink.entity.ServicioDeportivo;
import com.example.telelink.entity.Usuario;
import com.example.telelink.repository.EspacioDeportivoRepository;
import com.example.telelink.repository.ReservaRepository;
import com.example.telelink.repository.ServicioDeportivoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final EspacioDeportivoRepository espacioDeportivoRepository;
    private final ServicioDeportivoRepository servicioDeportivoRepository;

    public ReservaService(
            ReservaRepository reservaRepository,
            EspacioDeportivoRepository espacioDeportivoRepository,
            ServicioDeportivoRepository servicioDeportivoRepository) {
        this.reservaRepository = reservaRepository;
        this.espacioDeportivoRepository = espacioDeportivoRepository;
        this.servicioDeportivoRepository = servicioDeportivoRepository;
    }

    @Transactional(readOnly = true)
    public List<EspacioDeportivo> checkAvailability(String servicioDeportivo, LocalDateTime start, LocalDateTime end) {
        if (servicioDeportivo == null || start == null || end == null || start.isAfter(end) || start.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Servicio deportivo, fecha de inicio y fin son obligatorios, y la fecha no puede estar en el pasado.");
        }
        ServicioDeportivo servicio = servicioDeportivoRepository.findByServicioDeportivo(servicioDeportivo);
        if (servicio == null) {
            throw new IllegalArgumentException("Servicio deportivo no encontrado: " + servicioDeportivo);
        }
        List<EspacioDeportivo> espacios = espacioDeportivoRepository.findByEstablecimientoAndServicio(null, servicio.getServicioDeportivoId());
        return espacios.stream()
                .filter(e -> e.getEstadoServicio() == EspacioDeportivo.EstadoServicio.operativo)
                .filter(e -> start.toLocalTime().isAfter(e.getHorarioApertura()) && end.toLocalTime().isBefore(e.getHorarioCierre()))
                .filter(e -> reservaRepository.countActiveReservationConflicts(e.getEspacioDeportivoId(), start, end) == 0)
                .toList();
    }

    @Transactional
    public Reserva createReserva(Usuario usuario, String servicioDeportivo, LocalDateTime start, LocalDateTime end) {
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no autenticado.");
        }
        if (servicioDeportivo == null || start == null || end == null || start.isAfter(end) || start.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Servicio deportivo, fecha de inicio y fin son obligatorios, y la fecha no puede estar en el pasado.");
        }
        ServicioDeportivo servicio = servicioDeportivoRepository.findByServicioDeportivo(servicioDeportivo);
        if (servicio == null) {
            throw new IllegalArgumentException("Servicio deportivo no encontrado: " + servicioDeportivo);
        }
        List<EspacioDeportivo> espacios = checkAvailability(servicioDeportivo, start, end);
        if (espacios.isEmpty()) {
            throw new IllegalArgumentException("No hay espacios disponibles para " + servicioDeportivo + " en el horario solicitado.");
        }
        EspacioDeportivo espacio = espacios.get(0); // Select first available space
        Reserva reserva = new Reserva();
        reserva.setUsuario(usuario);
        reserva.setEspacioDeportivo(espacio);
        reserva.setInicioReserva(start);
        reserva.setFinReserva(end);
        reserva.setEstado(Reserva.Estado.pendiente);
        reserva.setFechaCreacion(LocalDateTime.now());
        return reservaRepository.save(reserva);
    }

    @Transactional
    public String cancelReserva(Usuario usuario, Integer reservaId, String razonCancelacion) {
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no autenticado.");
        }
        Reserva reserva = reservaRepository.findByReservaId(reservaId);
        if (reserva == null) {
            throw new IllegalArgumentException("Reserva no encontrada.");
        }
        if (!reserva.getUsuario().getUsuarioId().equals(usuario.getUsuarioId())) {
            throw new IllegalArgumentException("No tienes permiso para cancelar esta reserva.");
        }
        if (reserva.getEstado() == Reserva.Estado.cancelada) {
            throw new IllegalArgumentException("La reserva ya está cancelada.");
        }
        if (reserva.getInicioReserva().isBefore(LocalDateTime.now().plusHours(48))) {
            throw new IllegalArgumentException("No se puede cancelar la reserva dentro de las 48 horas previas al inicio.");
        }
        String servicio = reserva.getEspacioDeportivo().getServicioDeportivo().getServicioDeportivo();
        String feeMessage = servicio.equals("Cancha de Fútbol Grass") ? "Tarifa de cancelación: S/30" :
                servicio.equals("Cancha de Fútbol Loza") ? "Tarifa de cancelación: S/15" : "";
        reserva.setEstado(Reserva.Estado.cancelada);
        reserva.setRazonCancelacion(razonCancelacion);
        reserva.setFechaActualizacion(LocalDateTime.now());
        reservaRepository.save(reserva);
        return "Reserva cancelada exitosamente. " + feeMessage + " Complete el pago de la tarifa en la pestaña correspondiente.";
    }

    @Transactional(readOnly = true)
    public List<Reserva> getUserReservas(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no autenticado.");
        }
        return reservaRepository.findByUsuarioOrderByInicioReservaDesc(usuario);
    }
}