package com.example.telelink.langchain4j;

import com.example.telelink.entity.EspacioDeportivo;
import com.example.telelink.entity.Reserva;
import com.example.telelink.entity.ServicioDeportivo;
import com.example.telelink.entity.Usuario;
import com.example.telelink.repository.EspacioDeportivoRepository;
import com.example.telelink.repository.ReservaRepository;
import com.example.telelink.repository.ServicioDeportivoRepository;
import com.example.telelink.service.ReservaService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Component
public class LangChain4jTools {

    private final ReservaService reservaService;
    private final HttpSession session;
    private final ServicioDeportivoRepository servicioDeportivoRepository;
    private final EspacioDeportivoRepository espacioDeportivoRepository;
    private final ReservaRepository reservaRepository;

    public LangChain4jTools(
            ReservaService reservaService,
            HttpSession session,
            ServicioDeportivoRepository servicioDeportivoRepository,
            EspacioDeportivoRepository espacioDeportivoRepository,
            ReservaRepository reservaRepository) {
        this.reservaService = reservaService;
        this.session = session;
        this.servicioDeportivoRepository = servicioDeportivoRepository;
        this.espacioDeportivoRepository = espacioDeportivoRepository;
        this.reservaRepository = reservaRepository;
    }

    @Tool("Lista los espacios deportivos disponibles para un servicio deportivo específico.")
    public String listEspaciosForServicio(
            @P("Nombre del servicio deportivo (e.g., Cancha de Futbol Grass)") String servicioDeportivo) {
        try {
            ServicioDeportivo servicio = servicioDeportivoRepository.findByServicioDeportivo(servicioDeportivo);
            if (servicio == null) {
                return "Servicio deportivo no encontrado: " + servicioDeportivo + ". Opciones: Cancha de Futbol Grass, Cancha de Futbol Loza, Cancha de Basquet, Cancha de Voley, Cancha Multipropósito.";
            }

            List<EspacioDeportivo> espacios = espacioDeportivoRepository.findByServicioDeportivo(servicio);
            if (espacios.isEmpty()) {
                return "No hay espacios disponibles para " + servicioDeportivo + ".";
            }

            StringBuilder response = new StringBuilder("Espacios disponibles para " + servicioDeportivo + ":\n");
            for (EspacioDeportivo espacio : espacios) {
                if (espacio.getEstadoServicio() == EspacioDeportivo.EstadoServicio.operativo) {
                    response.append("- ").append(espacio.getNombre())
                            .append(" (").append(espacio.getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre())
                            .append(", Geolocalización: ").append(espacio.getGeolocalizacion())
                            .append(", Precio por hora: S/").append(espacio.getPrecioPorHora())
                            .append(", Horario: ").append(espacio.getHorarioApertura()).append(" a ").append(espacio.getHorarioCierre())
                            .append(")\n");
                }
            }
            return response.length() > 0 ? response.toString() : "No hay espacios operativos para " + servicioDeportivo + ".";
        } catch (Exception e) {
            return "Error al listar espacios: " + e.getMessage();
        }
    }

    @Tool("Verifica la disponibilidad de un espacio deportivo para un rango de tiempo.")
    public String checkAvailability(
            @P("Nombre del espacio deportivo (e.g., Cancha Principal)") String espacioNombre,
            @P("Fecha y hora de inicio (YYYY-MM-DD HH:mm)") String start,
            @P("Fecha y hora de fin (YYYY-MM-DD HH:mm)") String end) {
        try {
            // Validate inputs
            if (espacioNombre == null || start == null || end == null) {
                return "Por favor, proporciona el nombre del espacio, fecha de inicio y fin.";
            }

            // Find EspacioDeportivo
            EspacioDeportivo espacio = espacioDeportivoRepository.findByNombre(espacioNombre);
            if (espacio == null) {
                return "Espacio no encontrado: " + espacioNombre;
            }
            if (espacio.getEstadoServicio() != EspacioDeportivo.EstadoServicio.operativo) {
                return espacioNombre + " no está operativo.";
            }

            // Parse dates
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime inicio = LocalDateTime.parse(start, formatter);
            LocalDateTime fin = LocalDateTime.parse(end, formatter);

            // Validate dates
            if (inicio.isBefore(LocalDateTime.now()) || fin.isBefore(LocalDateTime.now())) {
                return "Las fechas deben ser en el futuro.";
            }
            if (fin.isBefore(inicio) || fin.isEqual(inicio)) {
                return "La hora de fin debe ser posterior a la hora de inicio.";
            }
            if (inicio.toLocalTime().isBefore(espacio.getHorarioApertura()) || fin.toLocalTime().isAfter(espacio.getHorarioCierre())) {
                return "El horario solicitado está fuera del horario de operación (" + espacio.getHorarioApertura() + " a " + espacio.getHorarioCierre() + ").";
            }

            // Check for conflicting reservations
            List<Reserva> reservas = reservaRepository.findActiveReservationsForEspacio(espacio.getEspacioDeportivoId(), inicio, fin);
            if (!reservas.isEmpty()) {
                return espacioNombre + " no está disponible en el horario solicitado. Prueba con otro horario o espacio.";
            }

            return espacioNombre + " está disponible el " + start + " hasta " + end + ". Costo: S/" + espacio.getPrecioPorHora().multiply(BigDecimal.valueOf(Duration.between(inicio, fin).toHours())) + ".";
        } catch (DateTimeParseException e) {
            return "Formato de fecha inválido. Usa YYYY-MM-DD HH:mm.";
        } catch (Exception e) {
            return "Error al verificar disponibilidad: " + e.getMessage();
        }
    }

    @Tool("Crea una reserva para un espacio deportivo.")
    public String createReserva(
            @P("Nombre del espacio deportivo (e.g., Cancha Principal)") String espacioNombre,
            @P("Fecha y hora de inicio (YYYY-MM-DD HH:mm)") String start,
            @P("Fecha y hora de fin (YYYY-MM-DD HH:mm)") String end) {
        try {
            // Validate session
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null) {
                return "Por favor, inicia sesión para crear una reserva.";
            }

            // Validate availability
            String availability = checkAvailability(espacioNombre, start, end);
            if (!availability.contains("está disponible")) {
                return availability;
            }

            // Create reservation
            EspacioDeportivo espacio = espacioDeportivoRepository.findByNombre(espacioNombre);
            Reserva reserva = new Reserva();
            reserva.setUsuario(usuario);
            reserva.setEspacioDeportivo(espacio);
            reserva.setInicioReserva(LocalDateTime.parse(start, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            reserva.setFinReserva(LocalDateTime.parse(end, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            reserva.setEstado(Reserva.Estado.pendiente);
            reserva.setFechaCreacion(LocalDateTime.now());
            reservaRepository.save(reserva);

            return "Reserva creada (ID: " + reserva.getReservaId() + ") en estado pendiente. Por favor, completa el pago de S/" +
                    espacio.getPrecioPorHora().multiply(BigDecimal.valueOf(Duration.between(reserva.getInicioReserva(), reserva.getFinReserva()).toHours())) +
                    " en la pestaña de pagos.";
        } catch (DateTimeParseException e) {
            return "Formato de fecha inválido. Usa YYYY-MM-DD HH:mm.";
        } catch (Exception e) {
            return "Error al crear la reserva: " + e.getMessage();
        }
    }

    @Tool("Cancela una reserva existente.")
    public String cancelReserva(
            @P("ID de la reserva") Integer reservaId,
            @P("Razón de cancelación (opcional)") String razonCancelacion) {
        try {
            // Validate session
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null) {
                return "Por favor, inicia sesión para cancelar una reserva.";
            }

            // Find reservation
            Reserva reserva = reservaRepository.findByReservaId(reservaId);
            if (reserva == null || !reserva.getUsuario().getUsuarioId().equals(usuario.getUsuarioId())) {
                return "Reserva no encontrada o no pertenece al usuario.";
            }

            // Check cancellation window
            if (reserva.getInicioReserva().isBefore(LocalDateTime.now().plusHours(48))) {
                return "No se puede cancelar: la reserva comienza en menos de 48 horas. No es reembolsable.";
            }

            // Apply cancellation fee
            String servicio = reserva.getEspacioDeportivo().getServicioDeportivo().getServicioDeportivo();
            String fee = servicio.equals("Cancha de Futbol Loza") ? "S/15" : "S/30";
            reserva.setEstado(Reserva.Estado.cancelada);
            reserva.setRazonCancelacion(razonCancelacion != null ? razonCancelacion : "Cancelada por el usuario");
            reserva.setFechaActualizacion(LocalDateTime.now());
            reservaRepository.save(reserva);

            return "Reserva " + reservaId + " cancelada. Se aplicó una tarifa de " + fee + ". El reembolso (si aplica) se procesará en 7 días hábiles.";
        } catch (Exception e) {
            return "Error al cancelar la reserva: " + e.getMessage();
        }
    }
}