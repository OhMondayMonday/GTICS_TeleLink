package com.example.telelink.langchain4j;

import com.example.telelink.entity.EspacioDeportivo;
import com.example.telelink.entity.Reserva;
import com.example.telelink.entity.ServicioDeportivo;
import com.example.telelink.entity.Usuario;
import com.example.telelink.repository.EspacioDeportivoRepository;
import com.example.telelink.repository.ReservaRepository;
import com.example.telelink.repository.ServicioDeportivoRepository;
import com.example.telelink.repository.PagoRepository;
import com.example.telelink.repository.ReembolsoRepository;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class LangChain4jTools {

    private final HttpSession session;
    private final ServicioDeportivoRepository servicioDeportivoRepository;
    private final EspacioDeportivoRepository espacioDeportivoRepository;
    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;
    private final ReembolsoRepository reembolsoRepository;

    public LangChain4jTools(
            HttpSession session,
            ServicioDeportivoRepository servicioDeportivoRepository,
            EspacioDeportivoRepository espacioDeportivoRepository,
            ReservaRepository reservaRepository,
            PagoRepository pagoRepository,
            ReembolsoRepository reembolsoRepository) {
        this.session = session;
        this.servicioDeportivoRepository = servicioDeportivoRepository;
        this.espacioDeportivoRepository = espacioDeportivoRepository;
        this.reservaRepository = reservaRepository;
        this.pagoRepository = pagoRepository;
        this.reembolsoRepository = reembolsoRepository;
    }

    @Tool("Lista todos los tipos de canchas disponibles, mostrando su ID.")
    public String listAllServicios() {
        try {
            List<ServicioDeportivo> servicios = servicioDeportivoRepository.findAll();
            if (servicios.isEmpty()) {
                return "No hay servicios deportivos disponibles. Asegúrate de listar todos estos servicios deportivos para el usuario.";
            }
            StringBuilder response = new StringBuilder("Servicios deportivos disponibles:\n");
            for (ServicioDeportivo servicio : servicios) {
                if (servicio.getServicioDeportivo().startsWith("Cancha")) {
                    response.append("- ID: ").append(servicio.getServicioDeportivoId())
                            .append(", Nombre: ").append(servicio.getServicioDeportivo())
                            .append("\n");
                }
            }
            response.append("Asegúrate de listar todos estos servicios deportivos para el usuario.");
            return response.toString();
        } catch (Exception e) {
            return "Error al listar servicios deportivos: " + e.getMessage();
        }
    }

    @Tool("Lista todos los espacios deportivos disponibles para un tipo de cancha, mostrando su ID.")
    public String listEspaciosForServicio(
            @P("ID del tipo de cancha (servicio deportivo)") Integer servicioDeportivoId) {
        try {
            ServicioDeportivo servicio = servicioDeportivoRepository.findById(servicioDeportivoId).orElse(null);
            if (servicio == null) {
                return "Servicio deportivo no encontrado con ID: " + servicioDeportivoId + ". Asegúrate de listar todos estos espacios deportivos para el usuario.";
            }
            List<EspacioDeportivo> espacios = espacioDeportivoRepository.findByServicioDeportivo(servicio);
            if (espacios.isEmpty()) {
                return "No hay espacios deportivos disponibles para " + servicio.getServicioDeportivo() + ". Asegúrate de listar todos estos espacios deportivos para el usuario.";
            }
            StringBuilder response = new StringBuilder("Espacios deportivos disponibles para " + servicio.getServicioDeportivo() + ":\n");
            for (EspacioDeportivo espacio : espacios) {
                if (espacio.getEstadoServicio() == EspacioDeportivo.EstadoServicio.operativo) {
                    response.append("- ID: ").append(espacio.getEspacioDeportivoId())
                            .append(", Nombre: ").append(espacio.getNombre())
                            .append(" (Establecimiento: ").append(espacio.getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre())
                            .append(", Ubicación: ").append(espacio.getGeolocalizacion())
                            .append(", Precio por hora: S/").append(espacio.getPrecioPorHora())
                            .append(", Horario: ").append(espacio.getHorarioApertura()).append(" a ").append(espacio.getHorarioCierre())
                            .append(")\n");
                }
            }
            response.append("Asegúrate de listar todos estos espacios deportivos para el usuario.");
            return response.toString();
        } catch (Exception e) {
            return "Error al listar espacios deportivos: " + e.getMessage();
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
                return "Espacio deportivo no encontrado con ID: " + espacioId + ". Asegúrate de listar todos estos espacios deportivos para el usuario.";
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime inicio = LocalDateTime.parse(start, formatter);
            LocalDateTime fin = LocalDateTime.parse(end, formatter);
            if (inicio.isBefore(LocalDateTime.now()) || fin.isBefore(LocalDateTime.now())) {
                return "Las fechas deben ser en el futuro. Asegúrate de listar todos estos espacios deportivos para el usuario.";
            }
            if (fin.isBefore(inicio) || fin.isEqual(inicio)) {
                return "La hora de fin debe ser después de la hora de inicio. Asegúrate de listar todos estos espacios deportivos para el usuario.";
            }
            if (inicio.toLocalTime().isBefore(espacio.getHorarioApertura()) || fin.toLocalTime().isAfter(espacio.getHorarioCierre())) {
                return "El horario solicitado está fuera del horario de operación (" + espacio.getHorarioApertura() + " a " + espacio.getHorarioCierre() + "). Asegúrate de listar todos estos espacios deportivos para el usuario.";
            }
            List<Reserva> reservas = reservaRepository.findReservasEnRango(espacioId, inicio, fin);
            if (!reservas.isEmpty()) {
                StringBuilder conflicts = new StringBuilder("No disponible. Reservas en conflicto:\n");
                for (Reserva r : reservas) {
                    String apellidos = r.getUsuario() != null && r.getUsuario().getApellidos() != null ? r.getUsuario().getApellidos() : "(usuario desconocido)";
                    conflicts.append("- El usuario ").append(apellidos)
                            .append(" ha reservado de ").append(r.getInicioReserva()).append(" a ").append(r.getFinReserva()).append("\n");
                }
                // Buscar alternativas en otros espacios del mismo servicio deportivo
                ServicioDeportivo servicio = espacio.getServicioDeportivo();
                List<EspacioDeportivo> otrosEspacios = espacioDeportivoRepository.findByServicioDeportivo(servicio)
                        .stream()
                        .filter(e -> !e.getEspacioDeportivoId().equals(espacioId))
                        .filter(e -> e.getEstadoServicio() == EspacioDeportivo.EstadoServicio.operativo)
                        .collect(Collectors.toList());
                List<EspacioDeportivo> disponibles = new java.util.ArrayList<>();
                for (EspacioDeportivo otro : otrosEspacios) {
                    List<Reserva> reservasOtro = reservaRepository.findReservasEnRango(otro.getEspacioDeportivoId(), inicio, fin);
                    if (reservasOtro.isEmpty()
                        && !inicio.toLocalTime().isBefore(otro.getHorarioApertura())
                        && !fin.toLocalTime().isAfter(otro.getHorarioCierre())) {
                        disponibles.add(otro);
                    }
                }
                if (!disponibles.isEmpty()) {
                    conflicts.append("\nOtros espacios deportivos disponibles para el mismo servicio en ese horario:\n");
                    for (EspacioDeportivo disp : disponibles) {
                        conflicts.append("- ")
                                .append(disp.getNombre())
                                .append(" (Establecimiento: ").append(disp.getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre())
                                .append(", Ubicación: ").append(disp.getGeolocalizacion())
                                .append(", Precio por hora: S/").append(disp.getPrecioPorHora())
                                .append(", Horario: ").append(disp.getHorarioApertura()).append(" a ").append(disp.getHorarioCierre())
                                .append(", ID: ").append(disp.getEspacioDeportivoId())
                                .append(")\n");
                    }
                } else {
                    conflicts.append("\nNo hay otros espacios deportivos disponibles para el mismo servicio en ese horario.");
                }
                conflicts.append("\nAsegúrate de listar todos estos espacios deportivos para el usuario.");
                return conflicts.toString();
            }
            long hours = Duration.between(inicio, fin).toHours();
            return "Espacio disponible. ID: " + espacioId + ", Nombre: " + espacio.getNombre() + ", Costo: S/" +
                    espacio.getPrecioPorHora().multiply(BigDecimal.valueOf(hours)) + " por " + hours + " horas. Asegúrate de listar todos estos espacios deportivos para el usuario.";
        } catch (DateTimeParseException e) {
            return "Formato de fecha inválido. Usa YYYY-MM-DD HH:mm. Asegúrate de listar todos estos espacios deportivos para el usuario.";
        } catch (Exception e) {
            return "Error al verificar disponibilidad: " + e.getMessage() + " Asegúrate de listar todos estos espacios deportivos para el usuario.";
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

    @Tool("Lista las reservas confirmadas del usuario desde el momento actual hasta 1 mes en el futuro, mostrando espacio, establecimiento, fecha y horario. Útil para cancelar reservas.")
    public String listUserConfirmedFutureReservas() {
        try {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null) {
                return "Por favor, inicia sesión para ver tus reservas.";
            }
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime oneMonthLater = now.plusMonths(1);
            List<Reserva> reservas = reservaRepository.findByUsuarioAndEstado(usuario, Reserva.Estado.confirmada);
            List<Reserva> futuras = reservas.stream()
                .filter(r -> !r.getInicioReserva().isBefore(now) && !r.getInicioReserva().isAfter(oneMonthLater))
                .sorted((a, b) -> a.getInicioReserva().compareTo(b.getInicioReserva()))
                .toList();
            if (futuras.isEmpty()) {
                return "No tienes reservas confirmadas próximas. Si tienes reservas pendientes de pago, primero realiza el pago para que sean confirmadas.";
            }
            StringBuilder sb = new StringBuilder("Tus reservas confirmadas próximas:\n");
            for (Reserva r : futuras) {
                sb.append("- ID: ").append(r.getReservaId())
                  .append(", Espacio: ").append(r.getEspacioDeportivo().getNombre())
                  .append(", Establecimiento: ").append(r.getEspacioDeportivo().getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre())
                  .append(", Fecha: ").append(r.getInicioReserva().toLocalDate())
                  .append(", Horario: ").append(r.getInicioReserva().toLocalTime()).append(" a ").append(r.getFinReserva().toLocalTime())
                  .append("\n");
            }
            sb.append("Para cancelar una reserva, el usuario debe indicar el nombre del espacio deportivo, el establecimiento, la fecha y el horario de la reserva que desea cancelar para que lo asocies con una ID y puedas usar el Tool cancelReserva. Asegúrate de listar todos las reservas procimas confirmadas para el usuario.");
            return sb.toString();
        } catch (Exception e) {
            return "Error al listar reservas confirmadas: " + e.getMessage();
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
            if (reserva.getEstado() == Reserva.Estado.cancelada) {
                return "La reserva ya está cancelada.";
            }
            LocalDateTime ahora = LocalDateTime.now();
            LocalDateTime limite = reserva.getInicioReserva().minusHours(48);
            boolean dentroDe48Horas = ahora.isAfter(limite);
            reserva.setEstado(Reserva.Estado.cancelada);
            reserva.setRazonCancelacion(razonCancelacion != null ? razonCancelacion : "Cancelación por el usuario");
            reserva.setFechaActualizacion(LocalDateTime.now());
            reservaRepository.save(reserva);
            // Buscar el pago asociado a la reserva
            Optional<com.example.telelink.entity.Pago> pagoOptional = pagoRepository.findByReserva(reserva);
            String mensaje;
            if (pagoOptional.isPresent()) {
                com.example.telelink.entity.Pago pago = pagoOptional.get();
                if (!dentroDe48Horas) {
                    // Elegible para reembolso (cancelación con 48+ horas de anticipación)
                    com.example.telelink.entity.Reembolso reembolso = new com.example.telelink.entity.Reembolso();
                    reembolso.setMonto(pago.getMonto());
                    reembolso.setMotivo(razonCancelacion != null ? razonCancelacion : "Cancelación de reserva");
                    reembolso.setFechaReembolso(LocalDateTime.now());
                    reembolso.setPago(pago);
                    if (pago.getMetodoPago() != null && "Pago Online".equals(pago.getMetodoPago().getMetodoPago())) {
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
            return mensaje;
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