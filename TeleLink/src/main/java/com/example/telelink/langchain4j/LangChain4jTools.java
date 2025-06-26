package com.example.telelink.langchain4j;

import com.example.telelink.entity.EspacioDeportivo;
import com.example.telelink.entity.Reserva;
import com.example.telelink.entity.ServicioDeportivo;
import com.example.telelink.entity.Usuario;
import com.example.telelink.repository.EspacioDeportivoRepository;
import com.example.telelink.repository.ReservaRepository;
import com.example.telelink.repository.ServicioDeportivoRepository;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class LangChain4jTools {

    private final HttpSession session;
    private final ServicioDeportivoRepository servicioDeportivoRepository;
    private final EspacioDeportivoRepository espacioDeportivoRepository;
    private final ReservaRepository reservaRepository;

    public LangChain4jTools(
            HttpSession session,
            ServicioDeportivoRepository servicioDeportivoRepository,
            EspacioDeportivoRepository espacioDeportivoRepository,
            ReservaRepository reservaRepository) {
        this.session = session;
        this.servicioDeportivoRepository = servicioDeportivoRepository;
        this.espacioDeportivoRepository = espacioDeportivoRepository;
        this.reservaRepository = reservaRepository;
    }

    @Tool("Lista todos los tipos de canchas disponibles.")
    public String listAllServicios() {
        try {
            List<ServicioDeportivo> servicios = servicioDeportivoRepository.findAll();
            if (servicios.isEmpty()) {
                return "No hay tipos de canchas disponibles.";
            }
            return "Tipos de canchas disponibles: " +
                    servicios.stream()
                            .map(ServicioDeportivo::getServicioDeportivo)
                            .filter(s -> s.startsWith("Cancha")) // Only include supported services
                            .collect(Collectors.joining(", "));
        } catch (Exception e) {
            return "Error al listar tipos de canchas: " + e.getMessage();
        }
    }

    @Tool("Lista todos los espacios deportivos disponibles para un tipo de cancha.")
    public String listEspaciosForServicio(
            @P("Nombre del tipo de cancha (e.g., Cancha de Futbol Grass)") String servicioDeportivo) {
        try {
            String normalizedServicio = normalizeServicioDeportivo(servicioDeportivo);
            ServicioDeportivo servicio = servicioDeportivoRepository.findByServicioDeportivoIgnoreCase(normalizedServicio);
            if (servicio == null) {
                return "Tipo de cancha no encontrado: " + servicioDeportivo + ". Opciones: " + listAllServicios();
            }

            List<EspacioDeportivo> espacios = espacioDeportivoRepository.findByServicioDeportivo(servicio);
            if (espacios.isEmpty()) {
                return "No hay canchas disponibles para " + normalizedServicio + ".";
            }

            StringBuilder response = new StringBuilder("Canchas disponibles para " + normalizedServicio + ":\n");
            for (EspacioDeportivo espacio : espacios) {
                if (espacio.getEstadoServicio() == EspacioDeportivo.EstadoServicio.operativo) {
                    response.append("- ").append(espacio.getNombre())
                            .append(" (").append(espacio.getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre())
                            .append(", Ubicación: ").append(espacio.getGeolocalizacion())
                            .append(", Precio por hora: S/").append(espacio.getPrecioPorHora())
                            .append(", Horario: ").append(espacio.getHorarioApertura()).append(" a ").append(espacio.getHorarioCierre())
                            .append(")\n");
                }
            }
            return response.length() > 0 ? response.toString() : "No hay canchas operativas para " + normalizedServicio + ".";
        } catch (Exception e) {
            return "Error al listar canchas: " + e.getMessage();
        }
    }

    @Tool("Cuenta cuántos espacios deportivos existen para un tipo de cancha.")
    public String countEspaciosForServicio(
            @P("Nombre del tipo de cancha (e.g., Cancha de Futbol Grass)") String servicioDeportivo) {
        try {
            String normalizedServicio = normalizeServicioDeportivo(servicioDeportivo);
            ServicioDeportivo servicio = servicioDeportivoRepository.findByServicioDeportivoIgnoreCase(normalizedServicio);
            if (servicio == null) {
                return "Tipo de cancha no encontrado: " + servicioDeportivo + ". Opciones: " + listAllServicios();
            }

            List<EspacioDeportivo> espacios = espacioDeportivoRepository.findByServicioDeportivo(servicio);
            long count = espacios.stream().filter(e -> e.getEstadoServicio() == EspacioDeportivo.EstadoServicio.operativo).count();
            return "Hay " + count + " canchas operativas para " + normalizedServicio + ". ¿Quieres ver la lista completa?";
        } catch (Exception e) {
            return "Error al contar canchas: " + e.getMessage();
        }
    }

    @Tool("Verifica si una cancha específica está disponible para un rango de tiempo.")
    public String checkAvailability(
            @P("Nombre de la cancha específica (e.g., Cancha Principal)") String espacioNombre,
            @P("Fecha y hora de inicio (YYYY-MM-DD HH:mm)") String start,
            @P("Fecha y hora de fin (YYYY-MM-DD HH:mm)") String end) {
        try {
            if (espacioNombre == null || start == null || end == null) {
                return "Por favor, proporciona el nombre de la cancha, fecha de inicio y fin.";
            }

            List<EspacioDeportivo> espacios = espacioDeportivoRepository.findByNombreContainingIgnoreCase(espacioNombre);
            if (espacios.isEmpty()) {
                return "Cancha no encontrada: " + espacioNombre + ". Usa el nombre exacto o revisa las opciones disponibles.";
            }
            if (espacios.size() > 1) {
                return "Hay varias canchas con ese nombre. Por favor, elige:\n" +
                        espacios.stream()
                                .map(e -> "- " + e.getNombre() + " (" + e.getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre() + ")")
                                .collect(Collectors.joining("\n"));
            }
            EspacioDeportivo espacio = espacios.get(0);
            if (espacio.getEstadoServicio() != EspacioDeportivo.EstadoServicio.operativo) {
                return espacio.getNombre() + " no está operativa.";
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime inicio = LocalDateTime.parse(start, formatter);
            LocalDateTime fin = LocalDateTime.parse(end, formatter);

            if (inicio.isBefore(LocalDateTime.now()) || fin.isBefore(LocalDateTime.now())) {
                return "Las fechas deben ser en el futuro.";
            }
            if (fin.isBefore(inicio) || fin.isEqual(inicio)) {
                return "La hora de fin debe ser después de la hora de inicio.";
            }
            if (inicio.toLocalTime().isBefore(espacio.getHorarioApertura()) || fin.toLocalTime().isAfter(espacio.getHorarioCierre())) {
                return "El horario solicitado está fuera del horario de operación (" + espacio.getHorarioApertura() + " a " + espacio.getHorarioCierre() + ").";
            }

            List<Reserva> reservas = reservaRepository.findActiveReservationsForEspacio(espacio.getEspacioDeportivoId(), inicio, fin);
            if (!reservas.isEmpty()) {
                return espacio.getNombre() + " no está disponible en el horario solicitado. Prueba con otro horario o cancha.";
            }

            long hours = Duration.between(inicio, fin).toHours();
            return espacio.getNombre() + " está disponible el " + start + " hasta " + end + ". Costo: S/" +
                    espacio.getPrecioPorHora().multiply(BigDecimal.valueOf(hours)) + " (" + hours + " horas).";
        } catch (DateTimeParseException e) {
            return "Formato de fecha inválido. Usa YYYY-MM-DD HH:mm.";
        } catch (Exception e) {
            return "Error al verificar disponibilidad: " + e.getMessage();
        }
    }

    @Tool("Crea una reserva para una cancha específica.")
    public String createReserva(
            @P("Nombre de la cancha específica (e.g., Cancha Principal)") String espacioNombre,
            @P("Fecha y hora de inicio (YYYY-MM-DD HH:mm)") String start,
            @P("Fecha y hora de fin (YYYY-MM-DD HH:mm)") String end) {
        try {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null) {
                return "Por favor, inicia sesión para crear una reserva.";
            }

            String availability = checkAvailability(espacioNombre, start, end);
            if (!availability.contains("está disponible")) {
                return availability;
            }

            List<EspacioDeportivo> espacios = espacioDeportivoRepository.findByNombreContainingIgnoreCase(espacioNombre);
            EspacioDeportivo espacio = espacios.get(0);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime inicio = LocalDateTime.parse(start, formatter);
            LocalDateTime fin = LocalDateTime.parse(end, formatter);

            List<Reserva> userReservas = reservaRepository.findByUsuarioAndEspacioDeportivoAndTimeRange(
                    usuario, espacio.getEspacioDeportivoId(), inicio, fin);
            if (!userReservas.isEmpty()) {
                return "Ya tienes una reserva para " + espacio.getNombre() + " en este horario. Por favor, elige otro horario o cancha.";
            }

            Reserva reserva = new Reserva();
            reserva.setUsuario(usuario);
            reserva.setEspacioDeportivo(espacio);
            reserva.setInicioReserva(inicio);
            reserva.setFinReserva(fin);
            reserva.setEstado(Reserva.Estado.pendiente);
            reserva.setFechaCreacion(LocalDateTime.now());
            reservaRepository.save(reserva);

            long hours = Duration.between(inicio, fin).toHours();
            return "Reserva creada (ID: " + reserva.getReservaId() + ") para " + espacio.getNombre() +
                    " el " + start + " hasta " + end +
                    ". Costo: S/" + espacio.getPrecioPorHora().multiply(BigDecimal.valueOf(hours)) +
                    ". Por favor, completa el pago en la pestaña de pagos dentro de 48 horas para confirmarla.";
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
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null) {
                return "Por favor, inicia sesión para cancelar una reserva.";
            }

            Reserva reserva = reservaRepository.findByReservaId(reservaId);
            if (reserva == null || !reserva.getUsuario().getUsuarioId().equals(usuario.getUsuarioId())) {
                return "Reserva no encontrada o no pertenece al usuario.";
            }

            if (reserva.getInicioReserva().isBefore(LocalDateTime.now().plusHours(48))) {
                return "No se puede cancelar: la reserva comienza en menos de 48 horas. No es reembolsable.";
            }

            String servicio = reserva.getEspacioDeportivo().getServicioDeportivo().getServicioDeportivo();
            String fee = servicio.equals("Cancha de Futbol Loza") ? "S/15" : "S/30";
            reserva.setEstado(Reserva.Estado.cancelada);
            reserva.setRazonCancelacion(razonCancelacion != null ? razonCancelacion : "Cancelada por el usuario");
            reserva.setFechaActualizacion(LocalDateTime.now());
            reservaRepository.save(reserva);

            return "Reserva " + reservaId + " cancelada. Se aplicó una tarifa de " + fee +
                    ". El reembolso (si aplica) se procesará en 7 días hábiles.";
        } catch (Exception e) {
            return "Error al cancelar la reserva: " + e.getMessage();
        }
    }

    private String normalizeServicioDeportivo(String servicio) {
        String normalized = normalizeInput(servicio);
        return switch (normalized) {
            case "futbol", "fútbol", "cancha de futbol", "cancha de fútbol" ->
                    throw new IllegalArgumentException("Por favor, elige: Cancha de Futbol Grass o Cancha de Futbol Loza.");
            case "futbol grass", "fútbol grass", "cancha de futbol grass", "cancha de fútbol grass" -> "Cancha de Futbol Grass";
            case "futbol loza", "fútbol loza", "cancha de futbol loza", "cancha de fútbol loza" -> "Cancha de Futbol Loza";
            case "basquet", "básquet", "basketball", "cancha de basquet", "cancha de básquet", "cancha de basketball" -> "Cancha de Basquet";
            case "voley", "vóley", "cancha de voley", "cancha de vóley" -> "Cancha de Vóley";
            case "multiproposito", "multipropósito", "cancha multiproposito", "cancha multipropósito" -> "Cancha Multipropósito";
            case "piscina", "gimnasio", "pista de atletismo" ->
                    throw new IllegalArgumentException("Lo siento, no ofrecemos " + servicio + ". Prueba con Cancha de Futbol Grass, Cancha de Basquet, Cancha de Voley, Cancha de Futbol Loza, o Cancha Multipropósito.");
            default -> servicio;
        };
    }

    private String normalizeInput(String input) {
        if (input == null) return null;
        input = input.trim().toLowerCase();
        return input.replaceAll("[áàäâã]", "a")
                .replaceAll("[éèëê]", "e")
                .replaceAll("[íìïî]", "i")
                .replaceAll("[óòöôõ]", "o")
                .replaceAll("[úùüû]", "u")
                .replaceAll("\\s+", " ");
    }
}