package com.example.telelink.langchain4j;

import com.example.telelink.entity.EspacioDeportivo;
import com.example.telelink.entity.Reserva;
import com.example.telelink.service.ReservaService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

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

    @Tool("Checks availability of sports facilities for a given service, start, and end time.")
    public List<EspacioDeportivo> checkAvailability(
            @P("Sports service (e.g., Cancha de Fútbol Grass, Cancha de Vóley, Cancha de Básquet)") String servicioDeportivo,
            @P("Start date and time in YYYY-MM-DD HH:mm format") String startDateTime,
            @P("End date and time in YYYY-MM-DD HH:mm format") String endDateTime) {
        try {
            LocalDateTime start = LocalDateTime.parse(startDateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            LocalDateTime end = LocalDateTime.parse(endDateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            return reservaService.checkAvailability(servicioDeportivo, start, end);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Formato de fecha inválido. Usa YYYY-MM-DD HH:mm.");
        }
    }

    @Tool("Creates a new reservation for a sports facility.")
    public Reserva createReserva(
            @P("Sports service (e.g., Cancha de Fútbol Grass, Cancha de Vóley, Cancha de Básquet)") String servicioDeportivo,
            @P("Start date and time in YYYY-MM-DD HH:mm format") String startDateTime,
            @P("End date and time in YYYY-MM-DD HH:mm format") String endDateTime) {
        try {
            LocalDateTime start = LocalDateTime.parse(startDateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            LocalDateTime end = LocalDateTime.parse(endDateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            return reservaService.createReserva(servicioDeportivo, start, end);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Formato de fecha inválido. Usa YYYY-MM-DD HH:mm.");
        }
    }

    @Tool("Cancels an existing reservation.")
    public String cancelReserva(
            @P("Reservation ID") Integer reservaId,
            @P("Cancellation reason (optional)") String razonCancelacion) {
        try {
            reservaService.cancelReserva(reservaId, razonCancelacion != null ? razonCancelacion : "Cancelación por el usuario");
            return "Reserva cancelada exitosamente.";
        } catch (IllegalArgumentException | IllegalStateException e) {
            return "Error al cancelar la reserva: " + e.getMessage();
        }
    }
}