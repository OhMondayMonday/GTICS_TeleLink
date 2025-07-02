package com.example.telelink.service;

import com.example.telelink.entity.EspacioDeportivo;
import com.example.telelink.entity.Reserva;
import com.example.telelink.entity.ServicioDeportivo;
import com.example.telelink.entity.Usuario;
import com.example.telelink.repository.EspacioDeportivoRepository;
import com.example.telelink.repository.ReservaRepository;
import com.example.telelink.repository.ServicioDeportivoRepository;
import com.example.telelink.repository.PagoRepository;
import com.example.telelink.repository.ReembolsoRepository;
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
    private final PagoRepository pagoRepository;
    private final ReembolsoRepository reembolsoRepository;

    public ReservaService(
            ReservaRepository reservaRepository,
            EspacioDeportivoRepository espacioDeportivoRepository,
            ServicioDeportivoRepository servicioDeportivoRepository,
            PagoRepository pagoRepository,
            ReembolsoRepository reembolsoRepository) {
        this.reservaRepository = reservaRepository;
        this.espacioDeportivoRepository = espacioDeportivoRepository;
        this.servicioDeportivoRepository = servicioDeportivoRepository;
        this.pagoRepository = pagoRepository;
        this.reembolsoRepository = reembolsoRepository;
    }

    @Transactional(readOnly = true)
    public List<EspacioDeportivo> checkAvailabilityByServicioId(Integer servicioDeportivoId, LocalDateTime start, LocalDateTime end) {
        if (servicioDeportivoId == null || start == null || end == null || start.isAfter(end) || start.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Servicio deportivo, fecha de inicio y fin son obligatorios, y la fecha no puede estar en el pasado.");
        }
        ServicioDeportivo servicio = servicioDeportivoRepository.findById(servicioDeportivoId).orElse(null);
        if (servicio == null) {
            throw new IllegalArgumentException("Servicio deportivo no encontrado: " + servicioDeportivoId);
        }
        List<EspacioDeportivo> espacios = espacioDeportivoRepository.findByServicioDeportivo(servicio);
        return espacios.stream()
                .filter(e -> e.getEstadoServicio() == EspacioDeportivo.EstadoServicio.operativo)
                .filter(e -> start.toLocalTime().isAfter(e.getHorarioApertura()) && end.toLocalTime().isBefore(e.getHorarioCierre()))
                .filter(e -> reservaRepository.countActiveReservationConflicts(e.getEspacioDeportivoId(), start, end) == 0)
                .toList();
    }

    @Transactional
    public Reserva createReservaByEspacioId(Usuario usuario, Integer espacioDeportivoId, LocalDateTime start, LocalDateTime end) {
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no autenticado.");
        }
        if (espacioDeportivoId == null || start == null || end == null || start.isAfter(end) || start.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Espacio deportivo, fecha de inicio y fin son obligatorios, y la fecha no puede estar en el pasado.");
        }
        EspacioDeportivo espacio = espacioDeportivoRepository.findById(espacioDeportivoId).orElse(null);
        if (espacio == null) {
            throw new IllegalArgumentException("Espacio deportivo no encontrado: " + espacioDeportivoId);
        }
        // Verificar disponibilidad para ese espacio
        if (start.toLocalTime().isBefore(espacio.getHorarioApertura()) || end.toLocalTime().isAfter(espacio.getHorarioCierre())) {
            throw new IllegalArgumentException("El horario solicitado está fuera del horario de operación.");
        }
        if (reservaRepository.countActiveReservationConflicts(espacioDeportivoId, start, end) > 0) {
            throw new IllegalArgumentException("El espacio no está disponible en el horario solicitado.");
        }
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
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime limite = reserva.getInicioReserva().minusHours(48);
        boolean dentroDe48Horas = ahora.isAfter(limite);
        reserva.setEstado(Reserva.Estado.cancelada);
        reserva.setRazonCancelacion(razonCancelacion != null ? razonCancelacion : "Cancelación por el usuario");
        reserva.setFechaActualizacion(LocalDateTime.now());
        reservaRepository.save(reserva);
        // Buscar el pago asociado a la reserva
        String mensaje;
        try {
            java.util.Optional<com.example.telelink.entity.Pago> pagoOptional = pagoRepository.findByReserva(reserva);
            if (pagoOptional.isPresent()) {
                com.example.telelink.entity.Pago pago = pagoOptional.get();
                if (!dentroDe48Horas) {
                    // Elegible para reembolso (cancelación con 48+ horas de anticipación)
                    com.example.telelink.entity.Reembolso reembolso = new com.example.telelink.entity.Reembolso();
                    reembolso.setMonto(pago.getMonto());
                    reembolso.setMotivo(razonCancelacion != null ? razonCancelacion : "Cancelación de reserva");
                    reembolso.setFechaReembolso(LocalDateTime.now());
                    reembolso.setPago(pago);
                    if (pago.getMetodoPago().getMetodoPago().equals("Pago Online")) {
                        reembolso.setEstado(com.example.telelink.entity.Reembolso.Estado.completado);
                        reembolso.setDetallesTransaccion("Reembolso procesado automáticamente para Pago Online");
                        mensaje = "Reserva cancelada y reembolso procesado automáticamente.";
                    } else {
                        reembolso.setEstado(com.example.telelink.entity.Reembolso.Estado.pendiente);
                        reembolso.setDetallesTransaccion("Esperando aprobación del administrador");
                        mensaje = "Reserva cancelada. El reembolso está pendiente de aprobación.";
                    }
                    reembolsoRepository.save(reembolso);
                } else {
                    mensaje = "Reserva cancelada, pero no se procesó reembolso debido a cancelación con menos de 48 horas.";
                }
            } else {
                mensaje = "Reserva cancelada correctamente. No se encontró un pago asociado.";
            }
        } catch (Exception e) {
            mensaje = "Reserva cancelada, pero ocurrió un error al procesar el reembolso: " + e.getMessage();
        }
        return mensaje;
    }

    @Transactional(readOnly = true)
    public List<Reserva> getUserReservas(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no autenticado.");
        }
        return reservaRepository.findByUsuarioOrderByInicioReservaDesc(usuario);
    }
}