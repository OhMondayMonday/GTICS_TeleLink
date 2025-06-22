package com.example.telelink.controller;

import com.example.telelink.entity.EspacioDeportivo;
import com.example.telelink.entity.Reserva;
import com.example.telelink.entity.Usuario;
import com.example.telelink.repository.EspacioDeportivoRepository;
import com.example.telelink.repository.ReservaRepository;
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

        // Obtener todas las reservas del espacio
        List<Reserva> reservas = reservaRepository.findByEspacioDeportivo_EspacioDeportivoId(id);

        // Aforo del gimnasio
        int aforoGimnasio = espacio.getAforoGimnasio();

        // Agrupar reservas por intervalo de tiempo (hora de inicio y fin)
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
                evento.put("title", "Mi reserva");

                evento.put("backgroundColor", "#4b8af3"); // Azul para reservas propias
                evento.put("borderColor", "#2a6edf");

                eventos.add(evento);
            }
        }

        // Crear eventos "fantasma" solo para horarios donde el aforo está completamente
        // ocupado
        for (Map.Entry<String, List<Reserva>> entry : reservasPorIntervalo.entrySet()) {
            List<Reserva> reservasEnIntervalo = entry.getValue();

            // Si el número de reservas en este intervalo iguala o supera el aforo, bloquear
            // el horario
            if (reservasEnIntervalo.size() >= aforoGimnasio) {
                // Obtener primera reserva del intervalo para referencia
                Reserva primeraReserva = reservasEnIntervalo.get(0);

                Map<String, Object> eventoCompleto = new HashMap<>();
                eventoCompleto.put("id", "filled-" + primeraReserva.getInicioReserva().toString());
                eventoCompleto.put("start", primeraReserva.getInicioReserva().toString());
                eventoCompleto.put("end", primeraReserva.getFinReserva().toString());
                eventoCompleto.put("title", "Aforo completo");
                eventoCompleto.put("backgroundColor", "#a0a0a0"); // Gris para horarios completos
                eventoCompleto.put("borderColor", "#808080");
                eventoCompleto.put("esPropia", false);
                eventoCompleto.put("rendering", "background");

                eventos.add(eventoCompleto);
            }
            // Se ha eliminado el bloque 'else' que mostraba eventos de ocupación parcial
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

        // Obtener todas las reservas del espacio
        List<Reserva> reservas = reservaRepository.findByEspacioDeportivo_EspacioDeportivoId(id);

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

                if (reserva.getNumeroParticipantesPiscina() != null && reserva.getNumeroParticipantesPiscina() > 1) {
                    evento.put("numeroParticipantesPiscina", reserva.getNumeroParticipantesPiscina());
                    evento.put("title",
                            evento.get("title") + " - " + reserva.getNumeroParticipantesPiscina() + " part.");
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
                if (r.getNumeroParticipantesPiscina() != null) {
                    totalParticipantes += r.getNumeroParticipantesPiscina();
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

        // Obtener todas las reservas del espacio y filtrar solo las futuras
        List<Reserva> reservas = reservaRepository.findByEspacioDeportivo_EspacioDeportivoId(id)
                .stream()
                .filter(reserva -> reserva.getFinReserva().isAfter(ahora))
                .collect(Collectors.toList());

        return reservas.stream().map(reserva -> {
            Map<String, Object> evento = new HashMap<>();
            evento.put("id", reserva.getReservaId().toString());
            evento.put("title", "Reservado");
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
    }

    // Endpoint para verificar disponibilidad de horario
    @GetMapping("verificar-disponibilidad")
    public Map<String, Object> verificarDisponibilidad(
            @RequestParam("espacioId") Integer espacioId,
            @RequestParam("inicio") String inicio,
            @RequestParam("fin") String fin,
            HttpSession session) {

        Map<String, Object> respuesta = new HashMap<>();

        try {
            // Obtener el espacio deportivo
            Optional<EspacioDeportivo> optEspacio = espacioDeportivoRepository.findById(espacioId);
            if (optEspacio.isEmpty()) {
                respuesta.put("disponible", false);
                respuesta.put("mensaje", "Espacio deportivo no encontrado");
                return respuesta;
            }

            EspacioDeportivo espacio = optEspacio.get();

            // Parsear fechas
            LocalDateTime inicioReserva = LocalDateTime.parse(inicio);
            LocalDateTime finReserva = LocalDateTime.parse(fin);

            // Verificar si el tipo de espacio es gimnasio
            if ("gimnasio".equalsIgnoreCase(espacio.getServicioDeportivo().getServicioDeportivo())) {
                // Para gimnasios, verificar el aforo
                int aforoGimnasio = espacio.getAforoGimnasio();
                long reservasEnHorario = reservaRepository.findByEspacioDeportivo_EspacioDeportivoId(espacioId)
                        .stream()
                        .filter(r -> r.getInicioReserva().isBefore(finReserva)
                                && r.getFinReserva().isAfter(inicioReserva))
                        .count();

                if (reservasEnHorario >= aforoGimnasio) {
                    respuesta.put("disponible", false);
                    respuesta.put("mensaje", "El aforo del gimnasio está completo para ese horario.");
                    return respuesta;
                }
            } else {
                // Para otros espacios, verificar conflictos
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

    @GetMapping("reservas/pista/{id}")
public List<Map<String, Object>> obtenerReservasPista(
        @PathVariable Integer id,
        HttpSession session) {
    // Obtener el usuario actual de la sesión
    Usuario usuarioActual = (Usuario) session.getAttribute("usuario");
    Integer usuarioId = usuarioActual != null ? usuarioActual.getUsuarioId() : null;
    
    // Obtener la fecha y hora actual
    LocalDateTime ahora = LocalDateTime.now();
    
    // Obtener el espacio deportivo para verificar que sea una pista
    Optional<EspacioDeportivo> espacioOpt = espacioDeportivoRepository.findById(id);
    if (!espacioOpt.isPresent()) {
        return List.of(); // Retornar lista vacía si no existe el espacio
    }
    
    EspacioDeportivo espacio = espacioOpt.get();
    boolean esPista = espacio.getServicioDeportivo() != null && 
                     "pista".equalsIgnoreCase(espacio.getServicioDeportivo().getServicioDeportivo());
    
    if (!esPista) {
        // Si no es pista, devolver lista vacía o error
        return List.of();
    }
    
    // Obtener todas las reservas del espacio
    List<Reserva> reservas = reservaRepository.findByEspacioDeportivo_EspacioDeportivoId(id);
    
    // Calcular capacidad total de la pista
    int carrilesPista = espacio.getCarrilesPista() != null ? espacio.getCarrilesPista() : 6; // Valor predeterminado
    int maxPersonasPorCarril = espacio.getMaxPersonasPorCarril() != null ? 
                              espacio.getMaxPersonasPorCarril() : 1; // Valor predeterminado
    int capacidadTotal = carrilesPista * maxPersonasPorCarril;
    
    // Agrupar reservas por intervalo de tiempo y carril
    Map<String, Map<Integer, List<Reserva>>> reservasPorIntervaloYCarril = new HashMap<>();
    
    for (Reserva reserva : reservas) {
        // Generar clave para el intervalo (inicio-fin)
        String intervaloKey = reserva.getInicioReserva().toString() + "-" + reserva.getFinReserva().toString();
        
        if (!reservasPorIntervaloYCarril.containsKey(intervaloKey)) {
            reservasPorIntervaloYCarril.put(intervaloKey, new HashMap<>());
        }
        
        // Obtener el carril (si es null, usar 1 como predeterminado)
        Integer carril = reserva.getNumeroCarrilPista() != null ? reserva.getNumeroCarrilPista() : 1;
        
        if (!reservasPorIntervaloYCarril.get(intervaloKey).containsKey(carril)) {
            reservasPorIntervaloYCarril.get(intervaloKey).put(carril, new ArrayList<>());
        }
        
        reservasPorIntervaloYCarril.get(intervaloKey).get(carril).add(reserva);
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
            
            // Agregar información de carril
            if (reserva.getNumeroCarrilPista() != null) {
                evento.put("numeroCarrilPista", reserva.getNumeroCarrilPista());
                evento.put("title", "Mi reserva (Carril " + reserva.getNumeroCarrilPista() + ")");
            } else {
                evento.put("title", "Mi reserva");
            }
            
            // Agregar información de participantes si aplica
            if (reserva.getNumeroParticipantesPiscina() != null && reserva.getNumeroParticipantesPiscina() > 1) {
                evento.put("numeroParticipantes", reserva.getNumeroParticipantesPiscina());
                evento.put("title", evento.get("title") + " - " + reserva.getNumeroParticipantesPiscina() + " part.");
            }
            
            evento.put("backgroundColor", "#4b8af3"); // Azul para reservas propias
            evento.put("borderColor", "#2a6edf");
            
            eventos.add(evento);
        }
    }
    
    // Crear eventos para carriles ocupados (para otros usuarios)
    for (Map.Entry<String, Map<Integer, List<Reserva>>> entryIntervalo : reservasPorIntervaloYCarril.entrySet()) {
        String intervaloKey = entryIntervalo.getKey();
        Map<Integer, List<Reserva>> reservasPorCarril = entryIntervalo.getValue();
        
        // Obtener primera reserva del intervalo para referencia de tiempo
        String[] partes = intervaloKey.split("-");
        LocalDateTime inicioIntervalo = LocalDateTime.parse(partes[0]);
        LocalDateTime finIntervalo = LocalDateTime.parse(partes[1]);
        
        // Para cada carril ocupado, crear un evento
        for (Map.Entry<Integer, List<Reserva>> entryCarril : reservasPorCarril.entrySet()) {
            Integer carril = entryCarril.getKey();
            List<Reserva> reservasCarril = entryCarril.getValue();
            
            // Si hay reservas que no son del usuario actual, mostrar el carril como ocupado
            boolean hayReservasAjenas = reservasCarril.stream()
                .anyMatch(r -> usuarioId == null || r.getUsuario() == null || 
                          !r.getUsuario().getUsuarioId().equals(usuarioId));
            
            if (hayReservasAjenas) {
                Map<String, Object> eventoCarrilOcupado = new HashMap<>();
                eventoCarrilOcupado.put("id", "carril-" + carril + "-" + inicioIntervalo.toString());
                eventoCarrilOcupado.put("start", inicioIntervalo.toString());
                eventoCarrilOcupado.put("end", finIntervalo.toString());
                eventoCarrilOcupado.put("title", "Carril " + carril + " ocupado");
                eventoCarrilOcupado.put("backgroundColor", "#a0a0a0"); // Gris para carriles ocupados
                eventoCarrilOcupado.put("borderColor", "#808080");
                eventoCarrilOcupado.put("esPropia", false);
                eventoCarrilOcupado.put("numeroCarrilPista", carril);
                
                eventos.add(eventoCarrilOcupado);
            }
        }
    }
    
    return eventos;
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
        Optional<EspacioDeportivo> espacioOpt = espacioDeportivoRepository.findById(espacioId);
        if (!espacioOpt.isPresent()) {
            respuesta.put("disponible", false);
            respuesta.put("mensaje", "Espacio deportivo no encontrado");
            return respuesta;
        }
        
        EspacioDeportivo espacio = espacioOpt.get();
        LocalDateTime inicioReserva = LocalDateTime.parse(inicio);
        LocalDateTime finReserva = LocalDateTime.parse(fin);
        
        // Verificar que sea una pista
        if (!"pista".equalsIgnoreCase(espacio.getServicioDeportivo().getServicioDeportivo())) {
            respuesta.put("disponible", false);
            respuesta.put("mensaje", "El espacio seleccionado no es una pista");
            return respuesta;
        }
        
        // Verificar límites de carril
        Integer totalCarriles = espacio.getCarrilesPista();
        if (totalCarriles == null) totalCarriles = 6; // Valor predeterminado
        
        if (carril < 1 || carril > totalCarriles) {
            respuesta.put("disponible", false);
            respuesta.put("mensaje", "Carril inválido. El rango válido es 1-" + totalCarriles);
            return respuesta;
        }
        
        // Verificar límite de participantes por carril
        Integer maxPersonasPorCarril = espacio.getMaxPersonasPorCarril();
        if (maxPersonasPorCarril == null) maxPersonasPorCarril = 1; // Valor predeterminado
        
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
            participantesActuales += (r.getNumeroParticipantesPiscina() != null) ? 
                                    r.getNumeroParticipantesPiscina() : 1;
        }
        
        // Verificar si hay espacio suficiente
        int espaciosDisponibles = maxPersonasPorCarril - participantesActuales;
        if (participantes > espaciosDisponibles) {
            respuesta.put("disponible", false);
            respuesta.put("mensaje", "No hay espacio suficiente en este carril. Espacios disponibles: " + espaciosDisponibles);
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
        
        // Obtener total de carriles y máximo de personas por carril
        Integer totalCarriles = espacio.getCarrilesPista();
        if (totalCarriles == null) totalCarriles = 6; // Valor predeterminado
        
        Integer maxPersonasPorCarril = espacio.getMaxPersonasPorCarril();
        if (maxPersonasPorCarril == null) maxPersonasPorCarril = 1; // Valor predeterminado
        
        // Obtener reservas para cada carril en ese horario
        Map<Integer, List<Reserva>> reservasPorCarril = new HashMap<>();
        for (int i = 1; i <= totalCarriles; i++) {
            reservasPorCarril.put(i, reservaRepository.findActiveReservationsForLanePista(
                    espacioId, inicioReserva, finReserva, i));
        }
        
        // Calcular información de cada carril
        Map<Integer, Map<String, Object>> carrilInfo = new HashMap<>();
        for (int i = 1; i <= totalCarriles; i++) {
            List<Reserva> reservasCarril = reservasPorCarril.get(i);
            
            // Calcular participantes actuales
            int participantesActuales = 0;
            for (Reserva r : reservasCarril) {
                participantesActuales += (r.getNumeroParticipantesPiscina() != null) ? 
                                       r.getNumeroParticipantesPiscina() : 1;
            }
            
            // Calcular espacios disponibles
            int espaciosDisponibles = maxPersonasPorCarril - participantesActuales;
            
            Map<String, Object> info = new HashMap<>();
            info.put("participantesActuales", participantesActuales);
            info.put("espaciosDisponibles", espaciosDisponibles);
            info.put("estaOcupado", participantesActuales > 0);
            info.put("estaLleno", espaciosDisponibles <= 0);
            
            carrilInfo.put(i, info);
        }
        
        // Preparar respuesta
        respuesta.put("totalCarriles", totalCarriles);
        respuesta.put("maxPersonasPorCarril", maxPersonasPorCarril);
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

            // Obtener el aforo total del gimnasio
            Integer aforoTotal = espacio.getAforoGimnasio();
            if (aforoTotal == null || aforoTotal <= 0) {
                aforoTotal = 40; // Valor por defecto si no está definido
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

            // Obtener todas las reservas activas para ese espacio en ese horario
            List<Reserva> reservasEnHorario = reservaRepository.findActiveReservationsInTimeRange(
                    espacioId, fechaHora, fechaHoraFin);

            // Contar el número de reservas (personas) en ese horario
            int personasInscritas = reservasEnHorario.size();

            // Calcular espacios disponibles
            int espaciosDisponibles = aforoTotal - personasInscritas;

            // Calcular porcentaje de ocupación
            double porcentajeOcupacion = ((double) personasInscritas / aforoTotal) * 100;

            // Preparar respuesta
            respuesta.put("aforoTotal", aforoTotal);
            respuesta.put("ocupacionActual", personasInscritas);
            respuesta.put("espaciosDisponibles", espaciosDisponibles);
            respuesta.put("porcentajeOcupacion", porcentajeOcupacion);
            respuesta.put("completo", espaciosDisponibles <= 0);

        } catch (Exception e) {
            respuesta.put("error", "Error al obtener disponibilidad: " + e.getMessage());
        }

        return respuesta;
    }
}
