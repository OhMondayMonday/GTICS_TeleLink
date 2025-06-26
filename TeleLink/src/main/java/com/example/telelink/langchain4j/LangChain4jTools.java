package com.example.telelink.langchain4j;

import com.example.telelink.entity.EspacioDeportivo;
import com.example.telelink.entity.Reserva;
import com.example.telelink.entity.Usuario;
import com.example.telelink.service.ReservaService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Component
public class LangChain4jTools {

    private final ReservaService reservaService;

    public LangChain4jTools(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    private Usuario getCurrentUser() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("No se encontró el contexto de la solicitud.");
        }
        HttpSession session = attributes.getRequest().getSession(false);
        if (session == null) {
            throw new IllegalStateException("No se encontró la sesión activa.");
        }
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            throw new IllegalStateException("Usuario no autenticado.");
        }
        return usuario;
    }

    @Tool("Checks availability of sports facilities for a given service, start, and end time.")
    public String checkAvailability(
            @P("Sports service (e.g., Cancha de Fútbol Grass, Cancha de Vóley, Cancha de Básquet)") String servicioDeportivo,
            @P("Start date and time in YYYY-MM-DD HH:mm format") String startDateTime,
            @P("End date and time in YYYY-MM-DD HH:mm format") String endDateTime) {
        try {
            LocalDateTime start = LocalDateTime.parse(startDateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            LocalDateTime end = LocalDateTime.parse(endDateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            List<EspacioDeportivo> espacios = reservaService.checkAvailability(servicioDeportivo, start, end);
            if (espacios.isEmpty()) {
                return "No hay espacios disponibles para " + servicioDeportivo + " en el horario solicitado.";
            }
            StringBuilder response = new StringBuilder("Espacios disponibles para " + servicioDeportivo + ":\n");
            for (EspacioDeportivo e : espacios) {
                response.append("- ").append(e.getNombre())
                        .append(" en ").append(e.getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre())
                        .append(", S/").append(e.getPrecioPorHora()).append(" por hora\n");
            }
            return response.toString();
        } catch (DateTimeParseException e) {
            return "Formato de fecha inválido. Usa YYYY-MM-DD HH:mm.";
        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool("Creates a new reservation for a sports facility.")
    public String createReserva(
            @P("Sports service (e.g., Cancha de Fútbol Grass, Cancha de Vóley, Cancha de Básquet)") String servicioDeportivo,
            @P("Start date and time in YYYY-MM-DD HH:mm format") String startDateTime,
            @P("End date and time in YYYY-MM-DD HH:mm format") String endDateTime) {
        try {
            LocalDateTime start = LocalDateTime.parse(startDateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            LocalDateTime end = LocalDateTime.parse(endDateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            Usuario usuario = getCurrentUser();
            Reserva reserva = reservaService.createReserva(usuario, servicioDeportivo, start, end);
            return "Reserva creada exitosamente (ID: " + reserva.getReservaId() + "). Por favor, completa el pago en la pestaña de pagos para confirmar.";
        } catch (DateTimeParseException e) {
            return "Formato de fecha inválido. Usa YYYY-MM-DD HH:mm.";
        } catch (IllegalArgumentException | IllegalStateException e) {
            return "Error al crear la reserva: " + e.getMessage();
        }
    }

    @Tool("Cancels an existing reservation.")
    public String cancelReserva(
            @P("Reservation ID") Integer reservaId,
            @P("Cancellation reason (optional)") String razonCancelacion) {
        try {
            Usuario usuario = getCurrentUser();
            String result = reservaService.cancelReserva(usuario, reservaId, razonCancelacion != null ? razonCancelacion : "Cancelación por el usuario");
            return result;
        } catch (IllegalArgumentException | IllegalStateException e) {
            return "Error al cancelar la reserva: " + e.getMessage();
        }
    }
}