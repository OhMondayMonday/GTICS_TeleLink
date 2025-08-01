package com.example.telelink.controller;

import com.example.telelink.entity.EspacioDeportivo;
import com.example.telelink.entity.Reserva;
import com.example.telelink.entity.Usuario;
import com.example.telelink.repository.EspacioDeportivoRepository;
import com.example.telelink.repository.ReservaRepository;
import com.example.telelink.repository.ReembolsoRepository;
import com.example.telelink.entity.Reembolso;
import com.example.telelink.repository.PagoRepository;
import com.example.telelink.entity.Pago;
import com.example.telelink.service.EmailService;
import com.example.telelink.entity.Mantenimiento;
import com.example.telelink.repository.MantenimientoRepository;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private EspacioDeportivoRepository espacioDeportivoRepository;

    @Autowired
    private ReembolsoRepository reembolsoRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private MantenimientoRepository mantenimientoRepository;

    // Método auxiliar para verificar si hay mantenimientos activos
    private boolean hayMantenimientoActivo(Integer espacioId, LocalDateTime inicio, LocalDateTime fin) {
        List<Mantenimiento> mantenimientosActivos = mantenimientoRepository
                .findByEspacioDeportivo_EspacioDeportivoIdAndEstadoInAndFechaInicioBeforeAndFechaEstimadaFinAfter(
                        espacioId,
                        List.of(Mantenimiento.Estado.pendiente, Mantenimiento.Estado.en_curso),
                        fin,
                        inicio);
        return !mantenimientosActivos.isEmpty();
    }

    // Método auxiliar para obtener mantenimientos que afectan un horario
    private List<Mantenimiento> obtenerMantenimientosEnHorario(Integer espacioId, LocalDateTime inicio,
            LocalDateTime fin) {
        return mantenimientoRepository
                .findByEspacioDeportivo_EspacioDeportivoIdAndEstadoInAndFechaInicioBeforeAndFechaEstimadaFinAfter(
                        espacioId,
                        List.of(Mantenimiento.Estado.pendiente, Mantenimiento.Estado.en_curso),
                        fin,
                        inicio);
    }

    @GetMapping("reservas/gimnasio/{id}")
    public List<Map<String, Object>> obtenerReservasGimnasio(
            @PathVariable Integer id,
            HttpSession session) {
        // Obtener el usuario actual de la sesión
        Usuario usuarioActual = (Usuario) session.getAttribute("usuario");
        Integer usuarioId = usuarioActual != null ? usuarioActual.getUsuarioId() : null;

        // Obtener la fecha y hora actual
        LocalDateTime ahora = LocalDateTime.now();

        // Obtener el espacio deportivo para verificar que sea un gimnasio
        Optional<EspacioDeportivo> espacioOpt = espacioDeportivoRepository.findById(id);
        if (!espacioOpt.isPresent()) {
            return List.of(); // Retornar lista vacía si no existe el espacio
        }

        EspacioDeportivo espacio = espacioOpt.get();
        boolean esGimnasio = espacio.getServicioDeportivo() != null &&
                "gimnasio".equalsIgnoreCase(espacio.getServicioDeportivo().getServicioDeportivo());

        if (!esGimnasio || espacio.getAforoGimnasio() == null) {
            // Si no es gimnasio o no tiene aforo definido, devolver lista vacía
            return List.of();
        }

        // Obtener todas las reservas del espacio QUE SEAN FUTURAS
        List<Reserva> reservas = reservaRepository.findByEspacioDeportivo_EspacioDeportivoId(id)
                .stream()
                .filter(r -> r.getFinReserva().isAfter(ahora)) // FILTRAR RESERVAS PASADAS
                .collect(Collectors.toList());

        // Filtrar reservas canceladas para aforo (importante para no contarlas)
        List<Reserva> reservasActivas = reservas.stream()
                .filter(r -> r.getEstado() != null && !r.getEstado().name().equalsIgnoreCase("CANCELADA"))
                .collect(Collectors.toList());

        // Aforo del gimnasio
        int aforoGimnasio = espacio.getAforoGimnasio();

        // Agrupar reservas ACTIVAS por intervalo de tiempo (hora de inicio y fin)
        Map<String, List<Reserva>> reservasPorIntervalo = new HashMap<>();

        for (Reserva reserva : reservasActivas) { // Solo consideramos reservas activas
            // Generar clave para el intervalo (inicio-fin)
            String intervaloKey = reserva.getInicioReserva().toString() + "-" + reserva.getFinReserva().toString();

            if (!reservasPorIntervalo.containsKey(intervaloKey)) {
                reservasPorIntervalo.put(intervaloKey, new ArrayList<>());
            }
            reservasPorIntervalo.get(intervaloKey).add(reserva);
        }

        List<Map<String, Object>> eventos = new ArrayList<>();

        // Crear eventos para las reservas propias (incluso las canceladas, solo para
        // mostrar)
        // IMPORTANTE: también filtramos aquí por reservas futuras
        for (Reserva reserva : reservas) { // Ya está filtrado por reservas futuras
            if (usuarioId != null && reserva.getUsuario() != null &&
                    reserva.getUsuario().getUsuarioId().equals(usuarioId)) {

                Map<String, Object> evento = new HashMap<>();
                evento.put("id", reserva.getReservaId().toString());
                evento.put("start", reserva.getInicioReserva().toString());
                evento.put("end", reserva.getFinReserva().toString());
                evento.put("esPropia", true);
                evento.put("estado", reserva.getEstado().name().toLowerCase());
                evento.put("title", "Mi reserva");

                // Añadir número de participantes en el título si es mayor a 1
                if (reserva.getNumeroParticipantes() != null && reserva.getNumeroParticipantes() > 1) {
                    evento.put("title", "Mi reserva (" + reserva.getNumeroParticipantes() + " personas)");
                    evento.put("numeroParticipantes", reserva.getNumeroParticipantes());
                }

                evento.put("backgroundColor", "#4b8af3"); // Azul para reservas propias
                evento.put("borderColor", "#2a6edf");

                // Si la reserva está cancelada, cambiar el color
                if (reserva.getEstado().name().equalsIgnoreCase("CANCELADA")) {
                    evento.put("backgroundColor", "#c0c0c0"); // Gris para reservas canceladas
                    evento.put("borderColor", "#a0a0a0");
                    evento.put("textColor", "#707070");
                }

                eventos.add(evento);
            }
        }

        // NUEVO: Agregar eventos de mantenimiento
        List<Mantenimiento> mantenimientos = mantenimientoRepository
                .findByEspacioDeportivo_EspacioDeportivoIdAndEstadoInAndFechaEstimadaFinAfter(
                        id,
                        List.of(Mantenimiento.Estado.pendiente, Mantenimiento.Estado.en_curso),
                        ahora);

        for (Mantenimiento mantenimiento : mantenimientos) {
            Map<String, Object> eventoMantenimiento = new HashMap<>();
            eventoMantenimiento.put("id", "mantenimiento-" + mantenimiento.getMantenimientoId());
            eventoMantenimiento.put("start", mantenimiento.getFechaInicio().toString());
            eventoMantenimiento.put("end", mantenimiento.getFechaEstimadaFin().toString());
            eventoMantenimiento.put("title", "Mantenimiento - " + mantenimiento.getMotivo());
            eventoMantenimiento.put("backgroundColor", "#ff6b6b"); // Rojo para mantenimientos
            eventoMantenimiento.put("borderColor", "#ff4757");
            eventoMantenimiento.put("textColor", "#ffffff");
            eventoMantenimiento.put("esMantenimiento", true);
            eventoMantenimiento.put("estado", mantenimiento.getEstado().name());
            eventoMantenimiento.put("display", "background"); // Evento de fondo para que cubra todo el día/horario
            eventoMantenimiento.put("rendering", "background");
            eventos.add(eventoMantenimiento);
        }

        return eventos;
    }

    @GetMapping("reservas/piscina/{id}")
    public List<Map<String, Object>> obtenerReservasPiscina(
            @PathVariable Integer id,
            HttpSession session) {
        // Obtener el usuario actual de la sesión
        Usuario usuarioActual = (Usuario) session.getAttribute("usuario");
        Integer usuarioId = usuarioActual != null ? usuarioActual.getUsuarioId() : null;

        // Obtener la fecha y hora actual
        LocalDateTime ahora = LocalDateTime.now();

        // Obtener el espacio deportivo para verificar que sea una piscina
        Optional<EspacioDeportivo> espacioOpt = espacioDeportivoRepository.findById(id);
        if (!espacioOpt.isPresent()) {
            return List.of(); // Retornar lista vacía si no existe el espacio
        }

        EspacioDeportivo espacio = espacioOpt.get();
        boolean esPiscina = espacio.getServicioDeportivo() != null &&
                "piscina".equalsIgnoreCase(espacio.getServicioDeportivo().getServicioDeportivo());

        if (!esPiscina) {
            // Si no es piscina, devolver lista vacía o error
            return List.of();
        }

        List<Reserva> reservas = reservaRepository.findByEspacioDeportivo_EspacioDeportivoId(id)
                .stream()
                .filter(r -> r.getFinReserva().isAfter(ahora)) // FILTRAR RESERVAS PASADAS
                .collect(Collectors.toList());

        // Calcular capacidad total de la piscina
        int capacidadTotal = espacio.getCarrilesPiscina() * espacio.getMaxPersonasPorCarril();

        // Agrupar reservas por intervalo de tiempo
        Map<String, List<Reserva>> reservasPorIntervalo = new HashMap<>();

        for (Reserva reserva : reservas) {
            // Generar clave para el intervalo (inicio-fin)
            String intervaloKey = reserva.getInicioReserva().toString() + "-" + reserva.getFinReserva().toString();

            if (!reservasPorIntervalo.containsKey(intervaloKey)) {
                reservasPorIntervalo.put(intervaloKey, new ArrayList<>());
            }
            reservasPorIntervalo.get(intervaloKey).add(reserva);
        }

        List<Map<String, Object>> eventos = new ArrayList<>();

        // Crear eventos para las reservas propias
        for (Reserva reserva : reservas) {
            if (usuarioId != null && reserva.getUsuario() != null &&
                    reserva.getUsuario().getUsuarioId().equals(usuarioId)) {

                Map<String, Object> evento = new HashMap<>();
                evento.put("id", reserva.getReservaId().toString());
                evento.put("start", reserva.getInicioReserva().toString());
                evento.put("end", reserva.getFinReserva().toString());
                evento.put("esPropia", true);
                evento.put("estado", reserva.getEstado().name().toLowerCase());

                // Agregar información de carril y participantes
                if (reserva.getNumeroCarrilPiscina() != null) {
                    evento.put("numeroCarrilPiscina", reserva.getNumeroCarrilPiscina());
                    evento.put("title", "Mi reserva (Carril " + reserva.getNumeroCarrilPiscina() + ")");
                } else {
                    evento.put("title", "Mi reserva");
                }

                if (reserva.numeroParticipantes() != null && reserva.numeroParticipantes() > 1) {
                    evento.put("numeroParticipantes", reserva.numeroParticipantes());
                    evento.put("title",
                            evento.get("title") + " - " + reserva.numeroParticipantes() + " part.");
                }

                evento.put("backgroundColor", "#4b8af3"); // Azul para reservas propias
                evento.put("borderColor", "#2a6edf");

                eventos.add(evento);
            }
        }

        // Crear eventos "fantasma" para horarios completamente ocupados
        for (Map.Entry<String, List<Reserva>> entry : reservasPorIntervalo.entrySet()) {
            List<Reserva> reservasEnIntervalo = entry.getValue();

            // Calcular el número total de participantes en este intervalo
            int totalParticipantes = 0;

            for (Reserva r : reservasEnIntervalo) {
                if (r.numeroParticipantes() != null) {
                    totalParticipantes += r.numeroParticipantes();
                } else {
                    totalParticipantes += 1; // Valor por defecto si no tiene participantes definidos
                }
            }

            // Si está completamente ocupado, crear evento fantasma
            if (totalParticipantes >= capacidadTotal) {
                // Obtener primera reserva del intervalo para referencia
                Reserva primeraReserva = reservasEnIntervalo.get(0);

                Map<String, Object> eventoCompletado = new HashMap<>();
                eventoCompletado.put("id", "filled-" + primeraReserva.getInicioReserva().toString());
                eventoCompletado.put("start", primeraReserva.getInicioReserva().toString());
                eventoCompletado.put("end", primeraReserva.getFinReserva().toString());
                eventoCompletado.put("title", "Completado");
                eventoCompletado.put("backgroundColor", "#a0a0a0"); // Gris para horarios completos
                eventoCompletado.put("borderColor", "#808080");
                eventoCompletado.put("esPropia", false);
                eventoCompletado.put("rendering", "background");

                eventos.add(eventoCompletado);
            }
        }

        // NUEVO: Agregar eventos de mantenimiento
        List<Mantenimiento> mantenimientos = mantenimientoRepository
                .findByEspacioDeportivo_EspacioDeportivoIdAndEstadoInAndFechaEstimadaFinAfter(
                        id,
                        List.of(Mantenimiento.Estado.pendiente, Mantenimiento.Estado.en_curso),
                        ahora);

        for (Mantenimiento mantenimiento : mantenimientos) {
            Map<String, Object> eventoMantenimiento = new HashMap<>();
            eventoMantenimiento.put("id", "mantenimiento-" + mantenimiento.getMantenimientoId());
            eventoMantenimiento.put("start", mantenimiento.getFechaInicio().toString());
            eventoMantenimiento.put("end", mantenimiento.getFechaEstimadaFin().toString());
            eventoMantenimiento.put("title", "Mantenimiento - " + mantenimiento.getMotivo());
            eventoMantenimiento.put("backgroundColor", "#ff6b6b"); // Rojo para mantenimientos
            eventoMantenimiento.put("borderColor", "#ff4757");
            eventoMantenimiento.put("textColor", "#ffffff");
            eventoMantenimiento.put("esMantenimiento", true);
            eventoMantenimiento.put("estado", mantenimiento.getEstado().name());
            eventoMantenimiento.put("display", "background");
            eventoMantenimiento.put("rendering", "background");
            eventos.add(eventoMantenimiento);
        }

        return eventos;
    }

    @GetMapping("reservas/espacio/{id}")
    public List<Map<String, Object>> obtenerReservasPorEspacio(
            @PathVariable Integer id,
            HttpSession session) {
        // Obtener el usuario actual de la sesión
        Usuario usuarioActual = (Usuario) session.getAttribute("usuario");
        Integer usuarioId = usuarioActual != null ? usuarioActual.getUsuarioId() : null;

        // Obtener la fecha y hora actual
        LocalDateTime ahora = LocalDateTime.now();

        // Obtener todas las reservas del espacio y filtrar:
        // 1. Solo reservas futuras
        // 2. Excluir reservas canceladas (excepto las propias, que se mostrarán con
        // estilo diferente)
        List<Reserva> reservas = reservaRepository.findByEspacioDeportivo_EspacioDeportivoId(id)
                .stream()
                .filter(reserva -> reserva.getFinReserva().isAfter(ahora))
                .filter(reserva -> {
                    // Incluir siempre las reservas propias (incluso canceladas) para que el usuario
                    // las vea
                    if (usuarioId != null && reserva.getUsuario() != null &&
                            reserva.getUsuario().getUsuarioId().equals(usuarioId)) {
                        return true;
                    }
                    // Para reservas de otros usuarios, excluir las canceladas
                    return reserva.getEstado() != null &&
                            !reserva.getEstado().name().equalsIgnoreCase("CANCELADA");
                })
                .collect(Collectors.toList());

        List<Map<String, Object>> eventos = reservas.stream().map(reserva -> {
            Map<String, Object> evento = new HashMap<>();
            evento.put("id", reserva.getReservaId().toString());
            evento.put("start", reserva.getInicioReserva().toString());
            evento.put("end", reserva.getFinReserva().toString());

            // Comprobar si la reserva pertenece al usuario actual
            boolean esReservaPropia = usuarioId != null &&
                    reserva.getUsuario() != null &&
                    reserva.getUsuario().getUsuarioId().equals(usuarioId);

            // Asignar color según si es reserva propia o ajena
            if (esReservaPropia) {
                evento.put("backgroundColor", "#4b8af3"); // Azul para reservas propias
                evento.put("borderColor", "#2a6edf");
                evento.put("esPropia", true);
                evento.put("title", "Mi reserva");

                // Si la reserva propia está cancelada, mostrarla con un estilo diferente
                if (reserva.getEstado() != null &&
                        reserva.getEstado().name().equalsIgnoreCase("CANCELADA")) {
                    evento.put("backgroundColor", "#c0c0c0"); // Gris para reservas canceladas
                    evento.put("borderColor", "#a0a0a0");
                    evento.put("textColor", "#707070");
                    evento.put("title", "Mi reserva (cancelada)");
                }
            } else {
                evento.put("backgroundColor", "#a0a0a0"); // Gris para reservas ajenas
                evento.put("borderColor", "#808080");
                evento.put("esPropia", false);
                evento.put("title", "Reservado");
            }

            // Agregar información del estado de la reserva para reservas propias
            if (esReservaPropia) {
                evento.put("estado", reserva.getEstado().name().toLowerCase());
            }

            return evento;
        }).collect(Collectors.toList());

        // NUEVO: Agregar eventos de mantenimiento
        List<Mantenimiento> mantenimientos = mantenimientoRepository
                .findByEspacioDeportivo_EspacioDeportivoIdAndEstadoInAndFechaEstimadaFinAfter(
                        id,
                        List.of(Mantenimiento.Estado.pendiente, Mantenimiento.Estado.en_curso),
                        ahora);

        for (Mantenimiento mantenimiento : mantenimientos) {
            Map<String, Object> eventoMantenimiento = new HashMap<>();
            eventoMantenimiento.put("id", "mantenimiento-" + mantenimiento.getMantenimientoId());
            eventoMantenimiento.put("start", mantenimiento.getFechaInicio().toString());
            eventoMantenimiento.put("end", mantenimiento.getFechaEstimadaFin().toString());
            eventoMantenimiento.put("title", "Mantenimiento - " + mantenimiento.getMotivo());
            eventoMantenimiento.put("backgroundColor", "#ff6b6b"); // Rojo para mantenimientos
            eventoMantenimiento.put("borderColor", "#ff4757");
            eventoMantenimiento.put("textColor", "#ffffff");
            eventoMantenimiento.put("esMantenimiento", true);
            eventoMantenimiento.put("estado", mantenimiento.getEstado().name());
            eventoMantenimiento.put("display", "background");
            eventoMantenimiento.put("rendering", "background");
            eventos.add(eventoMantenimiento);
        }

        return eventos;
    }

    @GetMapping("reservas/atletismo/{id}")
    public List<Map<String, Object>> obtenerReservasAtletismo(
            @PathVariable Integer id,
            HttpSession session) {
        // Obtener el usuario actual de la sesión
        Usuario usuarioActual = (Usuario) session.getAttribute("usuario");
        Integer usuarioId = usuarioActual != null ? usuarioActual.getUsuarioId() : null;

        // Obtener la fecha y hora actual
        LocalDateTime ahora = LocalDateTime.now();

        // Obtener el espacio deportivo para verificar que sea una pista de atletismo
        Optional<EspacioDeportivo> espacioOpt = espacioDeportivoRepository.findById(id);
        if (!espacioOpt.isPresent()) {
            return List.of(); // Retornar lista vacía si no existe el espacio
        }

        EspacioDeportivo espacio = espacioOpt.get();
        boolean esAtletismo = espacio.getServicioDeportivo() != null &&
                "atletismo".equalsIgnoreCase(espacio.getServicioDeportivo().getServicioDeportivo());

        if (!esAtletismo) {
            // Si no es pista de atletismo, devolver lista vacía o error
            return List.of();
        }

        List<Reserva> reservas = reservaRepository.findByEspacioDeportivo_EspacioDeportivoId(id)
                .stream()
                .filter(r -> r.getFinReserva().isAfter(ahora)) // FILTRAR RESERVAS PASADAS
                .collect(Collectors.toList());

        // Calcular capacidad total de la pista de atletismo
        int carrilesPista = espacio.getCarrilesPista() != null ? espacio.getCarrilesPista() : 8; // Valor predeterminado
        int maxPersonasPorCarril = espacio.getMaxPersonasPorCarril() != null ? espacio.getMaxPersonasPorCarril() : 1; // Valor
                                                                                                                      // predeterminado
        int capacidadTotal = carrilesPista * maxPersonasPorCarril;

        // Agrupar reservas por intervalo de tiempo
        Map<String, List<Reserva>> reservasPorIntervalo = new HashMap<>();

        for (Reserva reserva : reservas) {
            // Generar clave para el intervalo (inicio-fin)
            String intervaloKey = reserva.getInicioReserva().toString() + "-" + reserva.getFinReserva().toString();

            if (!reservasPorIntervalo.containsKey(intervaloKey)) {
                reservasPorIntervalo.put(intervaloKey, new ArrayList<>());
            }
            reservasPorIntervalo.get(intervaloKey).add(reserva);
        }

        List<Map<String, Object>> eventos = new ArrayList<>();

        // Crear eventos para las reservas propias
        for (Reserva reserva : reservas) {
            if (usuarioId != null && reserva.getUsuario() != null &&
                    reserva.getUsuario().getUsuarioId().equals(usuarioId)) {

                Map<String, Object> evento = new HashMap<>();
                evento.put("id", reserva.getReservaId().toString());
                evento.put("start", reserva.getInicioReserva().toString());
                evento.put("end", reserva.getFinReserva().toString());
                evento.put("esPropia", true);
                evento.put("estado", reserva.getEstado().name().toLowerCase());

                // Agregar información de carril y participantes
                if (reserva.getNumeroCarrilPista() != null) {
                    evento.put("numeroCarrilPista", reserva.getNumeroCarrilPista());
                    evento.put("title", "Mi reserva (Carril " + reserva.getNumeroCarrilPista() + ")");
                } else {
                    evento.put("title", "Mi reserva");
                }

                if (reserva.numeroParticipantes() != null && reserva.numeroParticipantes() > 1) {
                    evento.put("numeroParticipantes", reserva.numeroParticipantes());
                    evento.put("title",
                            evento.get("title") + " - " + reserva.numeroParticipantes() + " part.");
                }

                evento.put("backgroundColor", "#4b8af3"); // Azul para reservas propias
                evento.put("borderColor", "#2a6edf");

                eventos.add(evento);
            }
        }

        // Crear eventos "fantasma" para horarios completamente ocupados
        for (Map.Entry<String, List<Reserva>> entry : reservasPorIntervalo.entrySet()) {
            List<Reserva> reservasEnIntervalo = entry.getValue();

            // Calcular el número total de participantes en este intervalo
            int totalParticipantes = 0;

            for (Reserva r : reservasEnIntervalo) {
                if (r.numeroParticipantes() != null) {
                    totalParticipantes += r.numeroParticipantes();
                } else {
                    totalParticipantes += 1; // Valor por defecto si no tiene participantes definidos
                }
            }

            // Si está completamente ocupado, crear evento fantasma
            if (totalParticipantes >= capacidadTotal) {
                // Obtener primera reserva del intervalo para referencia
                Reserva primeraReserva = reservasEnIntervalo.get(0);

                Map<String, Object> eventoCompletado = new HashMap<>();
                eventoCompletado.put("id", "filled-" + primeraReserva.getInicioReserva().toString());
                eventoCompletado.put("start", primeraReserva.getInicioReserva().toString());
                eventoCompletado.put("end", primeraReserva.getFinReserva().toString());
                eventoCompletado.put("title", "Completado");
                eventoCompletado.put("backgroundColor", "#a0a0a0"); // Gris para horarios completos
                eventoCompletado.put("borderColor", "#808080");
                eventoCompletado.put("esPropia", false);
                eventoCompletado.put("rendering", "background");

                eventos.add(eventoCompletado);
            }
        }

        // NUEVO: Agregar eventos de mantenimiento
        List<Mantenimiento> mantenimientos = mantenimientoRepository
                .findByEspacioDeportivo_EspacioDeportivoIdAndEstadoInAndFechaEstimadaFinAfter(
                        id,
                        List.of(Mantenimiento.Estado.pendiente, Mantenimiento.Estado.en_curso),
                        ahora);

        for (Mantenimiento mantenimiento : mantenimientos) {
            Map<String, Object> eventoMantenimiento = new HashMap<>();
            eventoMantenimiento.put("id", "mantenimiento-" + mantenimiento.getMantenimientoId());
            eventoMantenimiento.put("start", mantenimiento.getFechaInicio().toString());
            eventoMantenimiento.put("end", mantenimiento.getFechaEstimadaFin().toString());
            eventoMantenimiento.put("title", "Mantenimiento - " + mantenimiento.getMotivo());
            eventoMantenimiento.put("backgroundColor", "#ff6b6b"); // Rojo para mantenimientos
            eventoMantenimiento.put("borderColor", "#ff4757");
            eventoMantenimiento.put("textColor", "#ffffff");
            eventoMantenimiento.put("esMantenimiento", true);
            eventoMantenimiento.put("estado", mantenimiento.getEstado().name());
            eventoMantenimiento.put("display", "background");
            eventoMantenimiento.put("rendering", "background");
            eventos.add(eventoMantenimiento);
        }

        return eventos;
    }

    @GetMapping("/verificar-disponibilidad-atletismo")
    public ResponseEntity<Map<String, Object>> verificarDisponibilidadAtletismo(
            @RequestParam("espacioId") Integer espacioId,
            @RequestParam("inicio") String inicio,
            @RequestParam("fin") String fin,
            @RequestParam("carril") Integer carril,
            @RequestParam("participantes") Integer participantes) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Parsear fechas
            LocalDateTime inicioLDT = parsearFechaISO(inicio);
            LocalDateTime finLDT = parsearFechaISO(fin);

            // Validar que inicio sea anterior a fin
            if (!inicioLDT.isBefore(finLDT)) {
                response.put("disponible", false);
                response.put("mensaje", "La hora de inicio debe ser anterior a la hora de fin");
                return ResponseEntity.badRequest().body(response);
            }

            // Validar que no sea en el pasado
            if (inicioLDT.isBefore(LocalDateTime.now())) {
                response.put("disponible", false);
                response.put("mensaje", "No se pueden hacer reservas en horarios pasados");
                return ResponseEntity.badRequest().body(response);
            }

            // Validar número de participantes
            if (participantes <= 0) {
                response.put("disponible", false);
                response.put("mensaje", "El número de participantes debe ser al menos 1");
                return ResponseEntity.badRequest().body(response);
            }

            // Obtener espacio deportivo
            Optional<EspacioDeportivo> espacioOpt = espacioDeportivoRepository.findById(espacioId);

            if (!espacioOpt.isPresent()) {
                response.put("disponible", false);
                response.put("mensaje", "Espacio deportivo no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            EspacioDeportivo espacio = espacioOpt.get();

            // Validar que el carril exista
            if (carril <= 0 || espacio.getCarrilesPista() == null || carril > espacio.getCarrilesPista()) {
                response.put("disponible", false);
                response.put("mensaje", "Carril no válido para esta pista de atletismo");
                return ResponseEntity.badRequest().body(response);
            }

            // Verificar disponibilidad de participantes en el carril
            int participantesActuales = reservaRepository.countTotalParticipantsInLaneAtletismo(
                    espacioId, inicioLDT, finLDT, carril);

            int espaciosDisponibles = espacio.getMaxPersonasPorCarril() - participantesActuales;

            if (participantes > espaciosDisponibles) {
                response.put("disponible", false);
                response.put("mensaje", "No hay suficientes espacios disponibles en este carril. Máximo disponible: "
                        + espaciosDisponibles);
                response.put("espaciosDisponibles", espaciosDisponibles);
            } else {
                response.put("disponible", true);
                response.put("mensaje", "Carril disponible para reserva");
                response.put("espaciosDisponibles", espaciosDisponibles);

                // Calcular precio
                BigDecimal precioPorHora = espacio.getPrecioPorHora();
                long horasReserva = Duration.between(inicioLDT, finLDT).toHours();

                if (horasReserva < 1)
                    horasReserva = 1; // Mínimo 1 hora

                BigDecimal precioTotal = precioPorHora
                        .multiply(BigDecimal.valueOf(horasReserva))
                        .multiply(BigDecimal.valueOf(participantes));

                response.put("precioTotal", precioTotal);
                response.put("horasReserva", horasReserva);
                response.put("precioPorHora", precioPorHora);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("disponible", false);
            response.put("mensaje", "Error al verificar disponibilidad: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Endpoint para verificar disponibilidad de horario
    @GetMapping("verificar-disponibilidad")
    public Map<String, Object> verificarDisponibilidad(
            @RequestParam("espacioId") Integer espacioId,
            @RequestParam("inicio") String inicio,
            @RequestParam("fin") String fin,
            @RequestParam(value = "participantes", required = false, defaultValue = "1") Integer participantes,
            HttpSession session) {

        Map<String, Object> respuesta = new HashMap<>();

        try {
            // Parsear fechas
            LocalDateTime inicioReserva = LocalDateTime.parse(inicio);
            LocalDateTime finReserva = LocalDateTime.parse(fin);

            // NUEVO: Verificar si hay mantenimientos activos en el horario solicitado
            if (hayMantenimientoActivo(espacioId, inicioReserva, finReserva)) {
                List<Mantenimiento> mantenimientos = obtenerMantenimientosEnHorario(espacioId, inicioReserva,
                        finReserva);
                String motivosMantenimiento = mantenimientos.stream()
                        .map(m -> m.getMotivo())
                        .collect(Collectors.joining(", "));

                respuesta.put("disponible", false);
                respuesta.put("mensaje",
                        "No se puede reservar durante el mantenimiento programado: " + motivosMantenimiento);
                respuesta.put("razon", "mantenimiento");
                respuesta.put("mantenimientos", mantenimientos.stream().map(m -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("motivo", m.getMotivo());
                    info.put("estado", m.getEstado().name());
                    info.put("fechaInicio", m.getFechaInicio().toString());
                    info.put("fechaFin", m.getFechaEstimadaFin().toString());
                    return info;
                }).collect(Collectors.toList()));
                return respuesta;
            }

            // Obtener el espacio deportivo
            Optional<EspacioDeportivo> optEspacio = espacioDeportivoRepository.findById(espacioId);
            if (optEspacio.isEmpty()) {
                respuesta.put("disponible", false);
                respuesta.put("mensaje", "Espacio deportivo no encontrado");
                return respuesta;
            }

            EspacioDeportivo espacio = optEspacio.get();

            // Verificar si el tipo de espacio es gimnasio
            if ("gimnasio".equalsIgnoreCase(espacio.getServicioDeportivo().getServicioDeportivo())) {
                // Para gimnasios, verificar el aforo
                int aforoGimnasio = espacio.getAforoGimnasio();

                // CAMBIO: Filtrar para excluir las reservas canceladas
                long reservasEnHorario = reservaRepository.findByEspacioDeportivo_EspacioDeportivoId(espacioId)
                        .stream()
                        .filter(r -> r.getInicioReserva().isBefore(finReserva)
                                && r.getFinReserva().isAfter(inicioReserva)
                                && r.getEstado() != null
                                && !r.getEstado().name().equalsIgnoreCase("CANCELADA"))
                        .count();

                if (reservasEnHorario >= aforoGimnasio) {
                    respuesta.put("disponible", false);
                    respuesta.put("mensaje", "El aforo del gimnasio está completo para ese horario.");
                    return respuesta;
                }

                // Verificar si hay suficiente espacio para el número de participantes
                if (participantes != null && participantes > 1) {
                    int espaciosDisponibles = (int) (aforoGimnasio - reservasEnHorario);
                    if (participantes > espaciosDisponibles) {
                        respuesta.put("disponible", false);
                        respuesta.put("mensaje", "No hay suficientes espacios disponibles para " + participantes +
                                " personas. Espacios disponibles: " + espaciosDisponibles);
                        respuesta.put("espaciosDisponibles", espaciosDisponibles);
                        return respuesta;
                    }
                }
            } else {
                // Para otros espacios, verificar conflictos excluyendo canceladas
                // Nota: Asumo que countActiveReservationConflicts ya excluye las canceladas
                // Si no es así, se necesitaría modificar también ese método
                long conflictos = reservaRepository.countActiveReservationConflicts(espacioId, inicioReserva,
                        finReserva);
                if (conflictos > 0) {
                    respuesta.put("disponible", false);
                    respuesta.put("mensaje",
                            "Ya existe otra reserva activa en este horario. Por favor, selecciona otro horario.");
                    return respuesta;
                }
            }

            // Si llegamos aquí, el horario está disponible
            respuesta.put("disponible", true);
            respuesta.put("mensaje", "Horario disponible");

        } catch (Exception e) {
            respuesta.put("disponible", false);
            respuesta.put("mensaje", "Error al verificar disponibilidad: " + e.getMessage());
        }

        return respuesta;
    }

    // Método auxiliar para parsear fechas ISO
    private LocalDateTime parsearFechaISO(String fechaISO) {
        // Manejar formato ISO con zona horaria (ej: "2025-06-11T16:00:00.000Z")
        if (fechaISO.endsWith("Z")) {
            // Remover la Z y convertir directamente manteniendo la hora UTC como local
            String fechaSinZ = fechaISO.substring(0, fechaISO.length() - 1);
            if (fechaSinZ.contains(".")) {
                fechaSinZ = fechaSinZ.substring(0, fechaSinZ.indexOf('.'));
            }
            return LocalDateTime.parse(fechaSinZ);
        } else {
            return LocalDateTime.parse(fechaISO);
        }
    }

    @GetMapping("/espacios/{id}/disponibilidad-carriles-atletismo")
    public ResponseEntity<Map<String, Object>> obtenerDisponibilidadCarrilesAtletismo(
            @PathVariable("id") Integer espacioId,
            @RequestParam("inicio") String inicio,
            @RequestParam("fin") String fin) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Parsear fechas
            LocalDateTime inicioLDT = parsearFechaISO(inicio);
            LocalDateTime finLDT = parsearFechaISO(fin);

            // Obtener espacio deportivo
            Optional<EspacioDeportivo> espacioOpt = espacioDeportivoRepository.findById(espacioId);

            if (!espacioOpt.isPresent()) {
                response.put("error", "Espacio deportivo no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            EspacioDeportivo espacio = espacioOpt.get();

            // Verificar que sea una pista de atletismo con carriles
            if (espacio.getCarrilesPista() == null || espacio.getCarrilesPista() <= 0 ||
                    espacio.getMaxPersonasPorCarril() == null || espacio.getMaxPersonasPorCarril() <= 0) {
                response.put("error", "Este espacio no es una pista de atletismo con carriles configurados");
                return ResponseEntity.badRequest().body(response);
            }

            // Mapa para almacenar información de cada carril
            Map<Integer, Map<String, Object>> infoCarriles = new HashMap<>();

            // Inicializar información para cada carril
            for (int i = 1; i <= espacio.getCarrilesPista(); i++) {
                Map<String, Object> carrilInfo = new HashMap<>();
                int participantesActuales = reservaRepository.countTotalParticipantsInLaneAtletismo(
                        espacioId, inicioLDT, finLDT, i);

                carrilInfo.put("participantesActuales", participantesActuales);
                carrilInfo.put("espaciosDisponibles", espacio.getMaxPersonasPorCarril() - participantesActuales);
                carrilInfo.put("maxParticipantes", espacio.getMaxPersonasPorCarril());

                infoCarriles.put(i, carrilInfo);
            }

            // Construir respuesta
            response.put("totalCarriles", espacio.getCarrilesPista());
            response.put("maxPersonasPorCarril", espacio.getMaxPersonasPorCarril());
            response.put("carrilInfo", infoCarriles);
            response.put("precioPorHora", espacio.getPrecioPorHora());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("error", "Error al obtener disponibilidad de carriles: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint para obtener la disponibilidad de carriles de una piscina en un
     * horario específico
     */
    @GetMapping("/espacios/{id}/disponibilidad-carriles")
    public ResponseEntity<Map<String, Object>> obtenerDisponibilidadCarriles(
            @PathVariable("id") Integer espacioId,
            @RequestParam("inicio") String inicio,
            @RequestParam("fin") String fin) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Parsear fechas
            LocalDateTime inicioLDT = parsearFechaISO(inicio);
            LocalDateTime finLDT = parsearFechaISO(fin);

            // Obtener espacio deportivo
            Optional<EspacioDeportivo> espacioOpt = espacioDeportivoRepository.findById(espacioId);

            if (!espacioOpt.isPresent()) {
                response.put("error", "Espacio deportivo no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            EspacioDeportivo espacio = espacioOpt.get();

            // Verificar que sea una piscina con carriles
            if (espacio.getCarrilesPiscina() == null || espacio.getCarrilesPiscina() <= 0 ||
                    espacio.getMaxPersonasPorCarril() == null || espacio.getMaxPersonasPorCarril() <= 0) {
                response.put("error", "Este espacio no es una piscina con carriles configurados");
                return ResponseEntity.badRequest().body(response);
            }

            // Mapa para almacenar información de cada carril
            Map<Integer, Map<String, Object>> infoCarriles = new HashMap<>();

            // Inicializar información para cada carril
            for (int i = 1; i <= espacio.getCarrilesPiscina(); i++) {
                Map<String, Object> carrilInfo = new HashMap<>();
                int participantesActuales = reservaRepository.countTotalParticipantsInLane(
                        espacioId, inicioLDT, finLDT, i);

                carrilInfo.put("participantesActuales", participantesActuales);
                carrilInfo.put("espaciosDisponibles", espacio.getMaxPersonasPorCarril() - participantesActuales);
                carrilInfo.put("maxParticipantes", espacio.getMaxPersonasPorCarril());

                infoCarriles.put(i, carrilInfo);
            }

            // Construir respuesta
            response.put("totalCarriles", espacio.getCarrilesPiscina());
            response.put("maxPersonasPorCarril", espacio.getMaxPersonasPorCarril());
            response.put("carrilInfo", infoCarriles);
            response.put("precioPorHora", espacio.getPrecioPorHora());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("error", "Error al obtener disponibilidad de carriles: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint para verificar disponibilidad específica de un carril de piscina
     */
    @GetMapping("/verificar-disponibilidad-piscina")
    public ResponseEntity<Map<String, Object>> verificarDisponibilidadPiscina(
            @RequestParam("espacioId") Integer espacioId,
            @RequestParam("inicio") String inicio,
            @RequestParam("fin") String fin,
            @RequestParam("carril") Integer carril,
            @RequestParam("participantes") Integer participantes) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Parsear fechas
            LocalDateTime inicioLDT = parsearFechaISO(inicio);
            LocalDateTime finLDT = parsearFechaISO(fin);

            // Validar que inicio sea anterior a fin
            if (!inicioLDT.isBefore(finLDT)) {
                response.put("disponible", false);
                response.put("mensaje", "La hora de inicio debe ser anterior a la hora de fin");
                return ResponseEntity.badRequest().body(response);
            }

            // Validar que no sea en el pasado
            if (inicioLDT.isBefore(LocalDateTime.now())) {
                response.put("disponible", false);
                response.put("mensaje", "No se pueden hacer reservas en horarios pasados");
                return ResponseEntity.badRequest().body(response);
            }

            // Validar número de participantes
            if (participantes <= 0) {
                response.put("disponible", false);
                response.put("mensaje", "El número de participantes debe ser al menos 1");
                return ResponseEntity.badRequest().body(response);
            }

            // Obtener espacio deportivo
            Optional<EspacioDeportivo> espacioOpt = espacioDeportivoRepository.findById(espacioId);

            if (!espacioOpt.isPresent()) {
                response.put("disponible", false);
                response.put("mensaje", "Espacio deportivo no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            EspacioDeportivo espacio = espacioOpt.get();

            // Validar que el carril exista
            if (carril <= 0 || espacio.getCarrilesPiscina() == null || carril > espacio.getCarrilesPiscina()) {
                response.put("disponible", false);
                response.put("mensaje", "Carril no válido para este espacio deportivo");
                return ResponseEntity.badRequest().body(response);
            }

            // Verificar disponibilidad de participantes en el carril
            int participantesActuales = reservaRepository.countTotalParticipantsInLane(
                    espacioId, inicioLDT, finLDT, carril);

            int espaciosDisponibles = espacio.getMaxPersonasPorCarril() - participantesActuales;

            if (participantes > espaciosDisponibles) {
                response.put("disponible", false);
                response.put("mensaje", "No hay suficientes espacios disponibles en este carril. Máximo disponible: "
                        + espaciosDisponibles);
                response.put("espaciosDisponibles", espaciosDisponibles);
            } else {
                response.put("disponible", true);
                response.put("mensaje", "Carril disponible para reserva");
                response.put("espaciosDisponibles", espaciosDisponibles);

                // Calcular precio
                BigDecimal precioPorHora = espacio.getPrecioPorHora();
                long horasReserva = Duration.between(inicioLDT, finLDT).toHours();

                if (horasReserva < 1)
                    horasReserva = 1; // Mínimo 1 hora

                BigDecimal precioTotal = precioPorHora
                        .multiply(BigDecimal.valueOf(horasReserva))
                        .multiply(BigDecimal.valueOf(participantes));

                response.put("precioTotal", precioTotal);
                response.put("horasReserva", horasReserva);
                response.put("precioPorHora", precioPorHora);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("disponible", false);
            response.put("mensaje", "Error al verificar disponibilidad: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/verificar-disponibilidad-pista")
    public Map<String, Object> verificarDisponibilidadPista(
            @RequestParam("espacioId") Integer espacioId,
            @RequestParam("inicio") String inicio,
            @RequestParam("fin") String fin,
            @RequestParam("carril") Integer carril,
            @RequestParam(value = "participantes", defaultValue = "1") Integer participantes) {

        Map<String, Object> respuesta = new HashMap<>();

        try {
            LocalDateTime inicioReserva = LocalDateTime.parse(inicio);
            LocalDateTime finReserva = LocalDateTime.parse(fin);

            // NUEVO: Verificar si hay mantenimientos activos en el horario solicitado
            if (hayMantenimientoActivo(espacioId, inicioReserva, finReserva)) {
                List<Mantenimiento> mantenimientos = obtenerMantenimientosEnHorario(espacioId, inicioReserva,
                        finReserva);
                String motivosMantenimiento = mantenimientos.stream()
                        .map(m -> m.getMotivo())
                        .collect(Collectors.joining(", "));

                respuesta.put("disponible", false);
                respuesta.put("mensaje",
                        "No se puede reservar durante el mantenimiento programado: " + motivosMantenimiento);
                respuesta.put("razon", "mantenimiento");
                return respuesta;
            }

            Optional<EspacioDeportivo> espacioOpt = espacioDeportivoRepository.findById(espacioId);
            if (!espacioOpt.isPresent()) {
                respuesta.put("disponible", false);
                respuesta.put("mensaje", "Espacio deportivo no encontrado");
                return respuesta;
            }

            EspacioDeportivo espacio = espacioOpt.get();

            // Verificar que sea una pista
            if (!"pista".equalsIgnoreCase(espacio.getServicioDeportivo().getServicioDeportivo())) {
                respuesta.put("disponible", false);
                respuesta.put("mensaje", "El espacio seleccionado no es una pista");
                return respuesta;
            }

            // Verificar límites de carril
            Integer totalCarriles = espacio.getCarrilesPista();
            if (totalCarriles == null)
                totalCarriles = 6; // Valor predeterminado

            if (carril < 1 || carril > totalCarriles) {
                respuesta.put("disponible", false);
                respuesta.put("mensaje", "Carril inválido. El rango válido es 1-" + totalCarriles);
                return respuesta;
            }

            // Verificar límite de participantes por carril
            Integer maxPersonasPorCarril = espacio.getMaxPersonasPorCarril();
            if (maxPersonasPorCarril == null)
                maxPersonasPorCarril = 1; // Valor predeterminado

            if (participantes < 1 || participantes > maxPersonasPorCarril) {
                respuesta.put("disponible", false);
                respuesta.put("mensaje", "Número de participantes inválido. El máximo es " + maxPersonasPorCarril);
                return respuesta;
            }

            // Obtener reservas para este carril y horario
            List<Reserva> reservasEnCarril = reservaRepository.findActiveReservationsForLanePista(
                    espacioId, inicioReserva, finReserva, carril);

            // Calcular el total de participantes actuales
            int participantesActuales = 0;
            for (Reserva r : reservasEnCarril) {
                participantesActuales += (r.numeroParticipantes() != null) ? r.numeroParticipantes() : 1;
            }

            // Verificar si hay espacio suficiente
            int espaciosDisponibles = maxPersonasPorCarril - participantesActuales;
            if (participantes > espaciosDisponibles) {
                respuesta.put("disponible", false);
                respuesta.put("mensaje",
                        "No hay espacio suficiente en este carril. Espacios disponibles: " + espaciosDisponibles);
                respuesta.put("espaciosDisponibles", espaciosDisponibles);
                return respuesta;
            }

            // Si llegamos aquí, hay disponibilidad
            respuesta.put("disponible", true);
            respuesta.put("mensaje", "Carril disponible");
            respuesta.put("espaciosDisponibles", espaciosDisponibles);

        } catch (Exception e) {
            respuesta.put("disponible", false);
            respuesta.put("mensaje", "Error al verificar disponibilidad: " + e.getMessage());
        }

        return respuesta;
    }

    @GetMapping("/espacios/{id}/disponibilidad-carriles-pista")
    public Map<String, Object> obtenerDisponibilidadCarrilesPista(
            @PathVariable("id") Integer espacioId,
            @RequestParam("inicio") String inicio,
            @RequestParam("fin") String fin) {

        Map<String, Object> respuesta = new HashMap<>();

        try {
            Optional<EspacioDeportivo> espacioOpt = espacioDeportivoRepository.findById(espacioId);
            if (!espacioOpt.isPresent()) {
                respuesta.put("error", "Espacio deportivo no encontrado");
                return respuesta;
            }

            EspacioDeportivo espacio = espacioOpt.get();

            // Verificar que sea una pista
            if (!"pista".equalsIgnoreCase(espacio.getServicioDeportivo().getServicioDeportivo())) {
                respuesta.put("error", "El espacio no es una pista");
                return respuesta;
            }

            LocalDateTime inicioReserva = LocalDateTime.parse(inicio);
            LocalDateTime finReserva = LocalDateTime.parse(fin);

            // Obtener reservas para cada carril en ese horario
            Map<Integer, List<Reserva>> reservasPorCarril = new HashMap<>();
            for (int i = 1; i <= espacio.getCarrilesPista(); i++) {
                reservasPorCarril.put(i, reservaRepository.findActiveReservationsForLanePista(
                        espacioId, inicioReserva, finReserva, i));
            }

            // Calcular información de cada carril
            Map<Integer, Map<String, Object>> carrilInfo = new HashMap<>();
            for (int i = 1; i <= espacio.getCarrilesPista(); i++) {
                List<Reserva> reservasCarril = reservasPorCarril.get(i);

                // Calcular participantes actuales
                int participantesActuales = 0;
                for (Reserva r : reservasCarril) {
                    participantesActuales += (r.numeroParticipantes() != null) ? r.numeroParticipantes() : 1;
                }

                // Calcular espacios disponibles
                int espaciosDisponibles = espacio.getMaxPersonasPorCarril() - participantesActuales;

                Map<String, Object> info = new HashMap<>();
                info.put("participantesActuales", participantesActuales);
                info.put("espaciosDisponibles", espaciosDisponibles);
                info.put("estaOcupado", participantesActuales > 0);
                info.put("estaLleno", espaciosDisponibles <= 0);

                carrilInfo.put(i, info);
            }

            // Preparar respuesta
            respuesta.put("totalCarriles", espacio.getCarrilesPista());
            respuesta.put("maxPersonasPorCarril", espacio.getMaxPersonasPorCarril());
            respuesta.put("carrilInfo", carrilInfo);
            respuesta.put("precioPorHora", espacio.getPrecioPorHora().toString());

        } catch (Exception e) {
            respuesta.put("error", "Error al obtener disponibilidad de carriles: " + e.getMessage());
        }

        return respuesta;
    }

    @GetMapping("/disponibilidad/gimnasio")
    public Map<String, Object> obtenerDisponibilidadGimnasio(
            @RequestParam("espacioId") Integer espacioId,
            @RequestParam("fecha") String fechaStr,
            @RequestParam("hora") String horaStr) {

        Map<String, Object> respuesta = new HashMap<>();

        try {
            // Obtener el espacio deportivo
            Optional<EspacioDeportivo> espacioOpt = espacioDeportivoRepository.findById(espacioId);
            if (!espacioOpt.isPresent()) {
                respuesta.put("error", "Espacio deportivo no encontrado");
                return respuesta;
            }

            EspacioDeportivo espacio = espacioOpt.get();

            // Verificar que sea un gimnasio
            if (!"gimnasio".equalsIgnoreCase(espacio.getServicioDeportivo().getServicioDeportivo())) {
                respuesta.put("error", "El espacio no es un gimnasio");
                return respuesta;
            }

            // Parsear la fecha y hora
            LocalDateTime fechaHora;
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                fechaHora = LocalDateTime.parse(fechaStr + "T" + horaStr, formatter);
            } catch (Exception e) {
                respuesta.put("error", "Formato de fecha u hora inválido");
                return respuesta;
            }

            // Calcular la hora de fin (una hora después)
            LocalDateTime fechaHoraFin = fechaHora.plusHours(1);

            // NUEVO: Verificar si hay mantenimientos activos en el horario
            if (hayMantenimientoActivo(espacioId, fechaHora, fechaHoraFin)) {
                List<Mantenimiento> mantenimientos = obtenerMantenimientosEnHorario(espacioId, fechaHora, fechaHoraFin);
                String motivosMantenimiento = mantenimientos.stream()
                        .map(m -> m.getMotivo())
                        .collect(Collectors.joining(", "));

                respuesta.put("error", "Mantenimiento en progreso: " + motivosMantenimiento);
                respuesta.put("hayMantenimiento", true);
                respuesta.put("motivosMantenimiento", motivosMantenimiento);
                return respuesta;
            }

            // Obtener el aforo total del gimnasio
            Integer aforoTotal = espacio.getAforoGimnasio();
            if (aforoTotal == null || aforoTotal <= 0) {
                aforoTotal = 40; // Valor por defecto si no está definido
            }

            // Obtener todas las reservas activas para ese espacio en ese horario
            List<Reserva> reservasEnHorario = reservaRepository.findActiveReservationsInTimeRange(
                    espacioId, fechaHora, fechaHoraFin);

            // CORRECCIÓN: Calcular el número total de participantes sumando el campo
            // numeroParticipantes
            int totalParticipantes = 0;
            for (Reserva reserva : reservasEnHorario) {
                // Sumar el número de participantes (o 1 si es null)
                int participantes = (reserva.getNumeroParticipantes() != null) ? reserva.getNumeroParticipantes() : 1;
                totalParticipantes += participantes;
            }

            // Calcular espacios disponibles basado en el total de participantes
            int espaciosDisponibles = aforoTotal - totalParticipantes;

            // Calcular porcentaje de ocupación
            double porcentajeOcupacion = ((double) totalParticipantes / aforoTotal) * 100;

            // Preparar respuesta
            respuesta.put("aforoTotal", aforoTotal);
            respuesta.put("ocupacionActual", totalParticipantes); // Ahora muestra el total de participantes
            respuesta.put("espaciosDisponibles", espaciosDisponibles);
            respuesta.put("porcentajeOcupacion", porcentajeOcupacion);
            respuesta.put("completo", espaciosDisponibles <= 0);
            respuesta.put("hayMantenimiento", false); // Añadir este campo para consistencia

        } catch (Exception e) {
            respuesta.put("error", "Error al obtener disponibilidad: " + e.getMessage());
        }

        return respuesta;
    }

    /**
     * Endpoint GET para cancelar una reserva desde el modal del calendario
     * Este endpoint funciona como alternativa al POST endpoint del
     * UsuarioController
     */
    @GetMapping("/cancelar-reserva-pendiente/{reservaId}")
    public ResponseEntity<?> cancelarReservaPendienteDesdeModal(
            @PathVariable("reservaId") Integer reservaId,
            @RequestParam(value = "razon", required = false, defaultValue = "Cancelación desde calendario") String razon,
            HttpSession session) {

        Map<String, Object> respuesta = new HashMap<>();

        try {
            // Obtener el usuario actual de la sesión
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null) {
                respuesta.put("error", "Usuario no autenticado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(respuesta);
            }

            // Buscar la reserva por ID
            Optional<Reserva> reservaOpt = reservaRepository.findById(reservaId);
            if (!reservaOpt.isPresent()) {
                respuesta.put("error", "Reserva no encontrada");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);
            }

            Reserva reserva = reservaOpt.get();

            // Verificar que la reserva pertenezca al usuario actual
            if (!reserva.getUsuario().getUsuarioId().equals(usuario.getUsuarioId())) {
                respuesta.put("error", "No tienes permiso para cancelar esta reserva");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(respuesta);
            }

            // Verificar el estado de la reserva y aplicar lógica correspondiente
            Reserva.Estado estadoActual = reserva.getEstado();

            // Limpiar y validar la razón
            String razonLimpia = razon != null ? razon.trim() : "";
            if (razonLimpia.isEmpty()) {
                razonLimpia = "Cancelación solicitada por el usuario";
            }
            if (razonLimpia.length() > 500) {
                razonLimpia = razonLimpia.substring(0, 500);
            }

            switch (estadoActual) {
                case pendiente:
                    // Para reservas pendientes: cancelación directa sin verificaciones adicionales
                    reserva.setEstado(Reserva.Estado.cancelada);
                    reserva.setRazonCancelacion(razonLimpia);
                    reserva.setFechaActualizacion(LocalDateTime.now());
                    reservaRepository.save(reserva);

                    // 🚀 ENVIAR CORREO DE CANCELACIÓN
                    try {
                        emailService.sendReservationCancelled(usuario, reserva, razonLimpia);
                        System.out.println(
                                "✅ Correo de cancelación enviado exitosamente a: " + usuario.getCorreoElectronico());
                    } catch (Exception emailException) {
                        System.err.println("❌ Error al enviar correo de cancelación: " + emailException.getMessage());
                    }

                    respuesta.put("success", true);
                    respuesta.put("mensaje", "Reserva pendiente cancelada exitosamente.");
                    respuesta.put("tipo", "cancelacion_directa");
                    respuesta.put("razonCancelacion", razonLimpia);
                    break;

                case confirmada:
                    // Para reservas confirmadas: verificar que sea 48 horas antes del inicio
                    LocalDateTime ahora = LocalDateTime.now();
                    LocalDateTime inicioReserva = reserva.getInicioReserva();
                    LocalDateTime limite48Horas = inicioReserva.minusHours(48);

                    if (ahora.isAfter(limite48Horas)) {
                        // No se puede cancelar: menos de 48 horas antes del inicio
                        respuesta.put("error", "No se puede cancelar la reserva confirmada. " +
                                "Las cancelaciones deben realizarse con al menos 48 horas de anticipación.");
                        respuesta.put("tipo", "tiempo_insuficiente");
                        respuesta.put("horasRestantes", java.time.Duration.between(ahora, inicioReserva).toHours());
                        respuesta.put("razonSolicitada", razonLimpia);
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
                    }

                    // Se puede cancelar: más de 48 horas antes del inicio
                    reserva.setEstado(Reserva.Estado.cancelada);
                    reserva.setRazonCancelacion(razonLimpia);
                    reserva.setFechaActualizacion(LocalDateTime.now());
                    reservaRepository.save(reserva);

                    // 🚀 ENVIAR CORREO DE CANCELACIÓN
                    try {
                        emailService.sendReservationCancelled(usuario, reserva, razonLimpia);
                        System.out.println("✅ Correo de cancelación (confirmada) enviado exitosamente a: "
                                + usuario.getCorreoElectronico());
                    } catch (Exception emailException) {
                        System.err.println("❌ Error al enviar correo de cancelación: " + emailException.getMessage());
                    }

                    // Buscar el pago asociado a la reserva para reembolso
                    Optional<Pago> pagoOpt = pagoRepository.findByReservaReservaId(reservaId);

                    if (pagoOpt.isPresent()) {
                        Pago pago = pagoOpt.get();

                        if (pago.getEstadoTransaccion() == Pago.EstadoTransaccion.completado &&
                                !reembolsoRepository.existsByPagoPagoId(pago.getPagoId())) {

                            // Crear el reembolso
                            Reembolso reembolso = new Reembolso();
                            reembolso.setPago(pago);
                            reembolso.setMonto(pago.getMonto());
                            reembolso.setEstado(Reembolso.Estado.pendiente);
                            reembolso.setMotivo("Cancelación de reserva confirmada - " + razonLimpia);
                            reembolso.setFechaReembolso(LocalDateTime.now());
                            reembolso.setDetallesTransaccion(
                                    "Reembolso automático por cancelación dentro del plazo permitido (48h). Razón: "
                                            + razonLimpia);

                            reembolsoRepository.save(reembolso);

                            respuesta.put("success", true);
                            respuesta.put("mensaje",
                                    "Reserva confirmada cancelada exitosamente. Se ha generado un reembolso por S/ " +
                                            pago.getMonto() + " que será procesado en los próximos días laborables.");
                            respuesta.put("tipo", "cancelacion_con_reembolso");
                            respuesta.put("montoReembolso", pago.getMonto());
                            respuesta.put("reembolsoId", reembolso.getReembolsoId());
                            respuesta.put("razonCancelacion", razonLimpia);
                        } else {
                            // Manejar casos donde no se puede generar reembolso automático
                            String mensajeAdicional = "";
                            if (pago.getEstadoTransaccion() != Pago.EstadoTransaccion.completado) {
                                mensajeAdicional = " El pago asociado no está completado.";
                            } else if (reembolsoRepository.existsByPagoPagoId(pago.getPagoId())) {
                                mensajeAdicional = " Ya existe un reembolso para este pago.";
                            }

                            respuesta.put("success", true);
                            respuesta.put("mensaje", "Reserva confirmada cancelada exitosamente." + mensajeAdicional +
                                    " Contacta con soporte si necesitas asistencia con el reembolso.");
                            respuesta.put("tipo", "cancelacion_sin_reembolso_automatico");
                            respuesta.put("razonCancelacion", razonLimpia);
                        }
                    } else {
                        respuesta.put("success", true);
                        respuesta.put("mensaje", "Reserva confirmada cancelada exitosamente. " +
                                "No se encontró un pago asociado. Contacta con soporte si realizaste un pago.");
                        respuesta.put("tipo", "cancelacion_sin_pago");
                        respuesta.put("razonCancelacion", razonLimpia);
                    }
                    break;

                case cancelada:
                    respuesta.put("error", "La reserva ya está cancelada");
                    respuesta.put("tipo", "ya_cancelada");
                    respuesta.put("razonSolicitada", razonLimpia);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);

                case completada:
                    respuesta.put("error", "No se puede cancelar una reserva que ya ha sido completada");
                    respuesta.put("tipo", "completada");
                    respuesta.put("razonSolicitada", razonLimpia);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);

                case en_proceso:
                    respuesta.put("error", "No se puede cancelar una reserva que está en proceso");
                    respuesta.put("tipo", "en_proceso");
                    respuesta.put("razonSolicitada", razonLimpia);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);

                default:
                    respuesta.put("error", "Estado de reserva no válido para cancelación");
                    respuesta.put("tipo", "estado_invalido");
                    respuesta.put("razonSolicitada", razonLimpia);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
            }

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            System.err.println("Error al cancelar reserva - ID: " + reservaId + ", Error: " + e.getMessage());
            e.printStackTrace();

            respuesta.put("error", "Error interno del servidor: " + e.getMessage());
            respuesta.put("tipo", "error_servidor");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuesta);
        }
    }

    @GetMapping("/reserva/{reservaId}/ocupacion")
    public ResponseEntity<?> obtenerOcupacionReserva(
            @PathVariable Integer reservaId,
            HttpSession session) {

        Map<String, Object> respuesta = new HashMap<>();

        try {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null) {
                respuesta.put("error", "Usuario no autenticado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(respuesta);
            }

            Optional<Reserva> reservaOpt = reservaRepository.findById(reservaId);
            if (!reservaOpt.isPresent()) {
                respuesta.put("error", "Reserva no encontrada");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);
            }

            Reserva reserva = reservaOpt.get();

            // Verificar que la reserva pertenezca al usuario
            if (!reserva.getUsuario().getUsuarioId().equals(usuario.getUsuarioId())) {
                respuesta.put("error", "No tienes permiso para ver esta reserva");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(respuesta);
            }

            EspacioDeportivo espacio = reserva.getEspacioDeportivo();
            String tipoServicio = espacio.getServicioDeportivo().getServicioDeportivo().toLowerCase();

            LocalDateTime inicio = reserva.getInicioReserva();
            LocalDateTime fin = reserva.getFinReserva();

            if ("piscina".equals(tipoServicio) || "atletismo".equals(tipoServicio)) {
                // Para piscina y atletismo (por carriles)
                Integer numeroCarril = "piscina".equals(tipoServicio) ? reserva.getNumeroCarrilPiscina()
                        : reserva.getNumeroCarrilPista();

                if (numeroCarril != null) {
                    // Buscar todas las reservas activas en el mismo carril y horario
                    List<Reserva> reservasCarril;
                    if ("piscina".equals(tipoServicio)) {
                        reservasCarril = reservaRepository
                                .findByEspacioDeportivo_EspacioDeportivoIdAndInicioReservaBeforeAndFinReservaAfterAndNumeroCarrilPiscinaAndEstadoNot(
                                        espacio.getEspacioDeportivoId(), fin, inicio, numeroCarril,
                                        Reserva.Estado.cancelada);
                    } else {
                        reservasCarril = reservaRepository
                                .findByEspacioDeportivo_EspacioDeportivoIdAndInicioReservaBeforeAndFinReservaAfterAndNumeroCarrilPistaAndEstadoNot(
                                        espacio.getEspacioDeportivoId(), fin, inicio, numeroCarril,
                                        Reserva.Estado.cancelada);
                    }

                    // Contar participantes totales en el carril
                    int participantesTotales = 0;
                    for (Reserva r : reservasCarril) {
                        participantesTotales += (r.getNumeroParticipantes() != null) ? r.getNumeroParticipantes() : 1;
                    }

                    int maxPersonasPorCarril = espacio.getMaxPersonasPorCarril() != null
                            ? espacio.getMaxPersonasPorCarril()
                            : 1;
                    int espaciosDisponibles = maxPersonasPorCarril - participantesTotales;

                    respuesta.put("tipoOcupacion", "carril");
                    respuesta.put("numeroCarril", numeroCarril);
                    respuesta.put("participantesTotales", participantesTotales);
                    respuesta.put("capacidadMaxima", maxPersonasPorCarril);
                    respuesta.put("espaciosDisponibles", espaciosDisponibles);
                    respuesta.put("porcentajeOcupacion",
                            Math.round((double) participantesTotales / maxPersonasPorCarril * 100));
                }

            } else if ("gimnasio".equals(tipoServicio)) {
                // Para gimnasio (aforo total)
                List<Reserva> reservasActivas = reservaRepository
                        .findByEspacioDeportivo_EspacioDeportivoId(espacio.getEspacioDeportivoId())
                        .stream()
                        .filter(r -> r.getInicioReserva().isBefore(fin) &&
                                r.getFinReserva().isAfter(inicio) &&
                                r.getEstado() != Reserva.Estado.cancelada)
                        .collect(Collectors.toList());

                int participantesTotales = 0;
                for (Reserva r : reservasActivas) {
                    participantesTotales += (r.getNumeroParticipantes() != null) ? r.getNumeroParticipantes() : 1;
                }

                int aforoGimnasio = espacio.getAforoGimnasio();
                int espaciosDisponibles = aforoGimnasio - participantesTotales;

                respuesta.put("tipoOcupacion", "gimnasio");
                respuesta.put("participantesTotales", participantesTotales);
                respuesta.put("capacidadMaxima", aforoGimnasio);
                respuesta.put("espaciosDisponibles", espaciosDisponibles);
                respuesta.put("porcentajeOcupacion", Math.round((double) participantesTotales / aforoGimnasio * 100));

            } else {
                // Para otros tipos (fútbol, etc.) - ocupación completa
                long reservasActivas = reservaRepository.countActiveReservationConflicts(
                        espacio.getEspacioDeportivoId(), inicio, fin);

                respuesta.put("tipoOcupacion", "completo");
                respuesta.put("estaOcupado", reservasActivas > 0);
                respuesta.put("numeroReservas", reservasActivas);
            }

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            respuesta.put("error", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuesta);
        }
    }
}
