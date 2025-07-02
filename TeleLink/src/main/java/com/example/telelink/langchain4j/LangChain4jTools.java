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

    @Tool("Lista todos los tipos de canchas disponibles, mostrando su ID.")
    public String listAllServicios() {
        try {
            List<ServicioDeportivo> servicios = servicioDeportivoRepository.findAll();
            if (servicios.isEmpty()) {
                return "No hay tipos de canchas disponibles.";
            }
            StringBuilder response = new StringBuilder("Tipos de canchas disponibles:\n");
            for (ServicioDeportivo servicio : servicios) {
                if (servicio.getServicioDeportivo().startsWith("Cancha")) {
                    response.append("- ID: ").append(servicio.getServicioDeportivoId())
                            .append(", Nombre: ").append(servicio.getServicioDeportivo())
                            .append("\n");
                }
            }
            return response.length() > 0 ? response.toString() : "No hay tipos de canchas disponibles.";
        } catch (Exception e) {
            return "Error al listar tipos de canchas: " + e.getMessage();
        }
    }

    @Tool("Lista todos los espacios deportivos disponibles para un tipo de cancha, mostrando su ID.")
    public String listEspaciosForServicio(
            @P("ID del tipo de cancha (servicio deportivo)") Integer servicioDeportivoId) {
        try {
            ServicioDeportivo servicio = servicioDeportivoRepository.findById(servicioDeportivoId).orElse(null);
            if (servicio == null) {
                return "Tipo de cancha no encontrado con ID: " + servicioDeportivoId + ".";
            }
            List<EspacioDeportivo> espacios = espacioDeportivoRepository.findByServicioDeportivo(servicio);
            if (espacios.isEmpty()) {
                return "No hay canchas disponibles para " + servicio.getServicioDeportivo() + ".";
            }
            StringBuilder response = new StringBuilder("Canchas disponibles para " + servicio.getServicioDeportivo() + ":\n");
            for (EspacioDeportivo espacio : espacios) {
                if (espacio.getEstadoServicio() == EspacioDeportivo.EstadoServicio.operativo) {
                    response.append("- ID: ").append(espacio.getEspacioDeportivoId())
                            .append(", Nombre: ").append(espacio.getNombre())
                            .append(" (Establecimiento: ").append(espacio.getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre())
                            .append(", Ubicación: ").append(espacio.getGeolocalizacion())
                            .append(", Precio por hora: S/").append(espacio.getPrecioPorHora())
                            .append(", Horario: ")
                            .append(espacio.getHorarioApertura()).append(" a ").append(espacio.getHorarioCierre())
                            .append(")\n");
                }
            }
            return response.length() > 0 ? response.toString() : "No hay canchas operativas para " + servicio.getServicioDeportivo() + ".";
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

    @Tool("Verifica si un espacio deportivo específico está disponible para un rango de tiempo usando su ID.")
    public String checkAvailabilityById(
            @P("ID del espacio deportivo") Integer espacioId,
            @P("Fecha y hora de inicio (YYYY-MM-DD HH:mm)") String start,
            @P("Fecha y hora de fin (YYYY-MM-DD HH:mm)") String end) {
        try {
            EspacioDeportivo espacio = espacioDeportivoRepository.findById(espacioId).orElse(null);
            if (espacio == null) {
                return "Espacio deportivo no encontrado con ID: " + espacioId;
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
            List<Reserva> reservas = reservaRepository.findReservasEnRango(espacioId, inicio, fin);
            if (!reservas.isEmpty()) {
                StringBuilder conflicts = new StringBuilder("No disponible. Reservas en conflicto:\n");
                for (Reserva r : reservas) {
                    conflicts.append("- Reserva ID ").append(r.getReservaId())
                            .append(" de ").append(r.getInicioReserva()).append(" a ").append(r.getFinReserva()).append("\n");
                }
                return conflicts.toString();
            }
            long hours = Duration.between(inicio, fin).toHours();
            return "Espacio disponible. ID: " + espacioId + ", Nombre: " + espacio.getNombre() + ", Costo: S/" +
                    espacio.getPrecioPorHora().multiply(BigDecimal.valueOf(hours)) + " por " + hours + " horas.";
        } catch (DateTimeParseException e) {
            return "Formato de fecha inválido. Usa YYYY-MM-DD HH:mm.";
        } catch (Exception e) {
            return "Error al verificar disponibilidad: " + e.getMessage();
        }
    }

    @Tool("Crea una reserva para un espacio deportivo específico usando su ID.")
    public String createReservaById(
            @P("ID del espacio deportivo") Integer espacioId,
            @P("Fecha y hora de inicio (YYYY-MM-DD HH:mm)") String start,
            @P("Fecha y hora de fin (YYYY-MM-DD HH:mm)") String end) {
        try {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null) {
                return "Por favor, inicia sesión para crear una reserva.";
            }
            EspacioDeportivo espacio = espacioDeportivoRepository.findById(espacioId).orElse(null);
            if (espacio == null) {
                return "Espacio deportivo no encontrado con ID: " + espacioId;
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
            List<Reserva> reservas = reservaRepository.findReservasEnRango(espacioId, inicio, fin);
            if (!reservas.isEmpty()) {
                return "No se puede crear la reserva. Hay reservas en conflicto.";
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
            return "Reserva creada (ID: " + reserva.getReservaId() + ") para el espacio ID " + espacioId + " (" + espacio.getNombre() + ") de " + start + " a " + end + ". Costo: S/" + espacio.getPrecioPorHora().multiply(BigDecimal.valueOf(hours)) + ".";
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