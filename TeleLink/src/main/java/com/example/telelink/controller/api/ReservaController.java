package com.example.telelink.controller.api;

import com.example.telelink.dto.vecino.ReservaDTO;
import com.example.telelink.entity.EspacioDeportivo;
import com.example.telelink.entity.Reserva;
import com.example.telelink.entity.Usuario;
import com.example.telelink.repository.EspacioDeportivoRepository;
import com.example.telelink.repository.ReservaRepository;
import com.example.telelink.repository.UsuarioRepository;
import com.example.telelink.service.ReservaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {
    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EspacioDeportivoRepository espacioDeportivoRepository;

    @Autowired
    private ReservaService reservaService;

    @PostMapping("/guardar")
    public ResponseEntity<Map<String, Object>> guardarReserva(@RequestBody Reserva reserva) {
        try {
            // Guardar la reserva en la base de datos
            reservaService.guardarReserva(reserva);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    //Obtiene las reservas disponibles para un espacio deportivo en una fecha específica
    @GetMapping("/disponibilidad")
    public ResponseEntity<?> getDisponibilidad(
            @RequestParam Integer servicioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        try {
            // Buscar el espacio deportivo por su ID
            Optional<EspacioDeportivo> espacioOpt = espacioDeportivoRepository.findById(servicioId);
            if (!espacioOpt.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Espacio deportivo no encontrado"));
            }

            // Obtener todas las reservas para ese espacio en esa fecha
            LocalDateTime inicioDia = fecha.atStartOfDay();
            LocalDateTime finDia = fecha.atTime(23, 59, 59);

            List<Reserva> reservasDelDia = reservaRepository
                    .findByEspacioDeportivoAndInicioReservaBetweenAndEstadoNot(
                            espacioOpt.get(), inicioDia, finDia, Reserva.Estado.cancelada);

            // Crear mapa de horas ocupadas
            Map<Integer, Map<String, Object>> horasOcupadas = new HashMap<>();

            // Procesar cada reserva para obtener las horas
            for (Reserva reserva : reservasDelDia) {
                // Para cada hora entre inicio y fin de la reserva
                LocalTime horaInicio = reserva.getInicioReserva().toLocalTime();
                LocalTime horaFin = reserva.getFinReserva().toLocalTime();

                for (int hora = horaInicio.getHour(); hora < horaFin.getHour(); hora++) {
                    Map<String, Object> datosHora = new HashMap<>();
                    datosHora.put("disponible", false);

                    // Verificar si la reserva es del usuario actual
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null && auth.getName().equals(reserva.getUsuario().getCorreoElectronico())) {
                        datosHora.put("esUsuarioActual", true);
                    } else {
                        datosHora.put("esUsuarioActual", false);
                    }

                    horasOcupadas.put(hora, datosHora);
                }
            }

            return ResponseEntity.ok(horasOcupadas);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener disponibilidad: " + e.getMessage()));
        }
    }

    /**
     * Crea una nueva reserva
     */
    @PostMapping("/crear")
    public ResponseEntity<?> crearReserva(@RequestBody ReservaDTO reservaDTO) {
        try {
            // Obtener el usuario autenticado
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado"));
            }

            //Obtener el usuario de la base de datos
            String username = auth.getName();
            Optional<Usuario> usuarioOpt = usuarioRepository.findByNombres(username);
            if (!usuarioOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no encontrado"));
            }

            // Obtener el espacio deportivo
            Optional<EspacioDeportivo> espacioOpt = espacioDeportivoRepository.findById(reservaDTO.getServicioId());
            if (!espacioOpt.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Espacio deportivo no encontrado"));
            }

            // Validar que las horas estén disponibles
            // Esta validación debería ser más exhaustiva en un entorno de producción
            LocalDate fecha = reservaDTO.getFecha();
            List<Integer> horas = reservaDTO.getHoras();

            // Verificar que todas las horas solicitadas estén disponibles
            for (Integer hora : horas) {
                LocalDateTime inicioHora = fecha.atTime(hora, 0);
                LocalDateTime finHora = fecha.atTime(hora + 1, 0);

                // Buscar si hay alguna reserva que se solape con esta hora
                List<Reserva> reservasExistentes = reservaRepository
                        .findByEspacioDeportivoAndEstadoNotAndFinReservaAfterAndInicioReservaBefore(
                                espacioOpt.get(),
                                Reserva.Estado.cancelada,
                                inicioHora,
                                finHora
                        );

                if (!reservasExistentes.isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "La hora " + hora + ":00 ya está reservada"));
                }
            }

            // Lista para guardar todas las reservas creadas
            List<Reserva> reservasCreadas = horas.stream().map(hora -> {
                // Crear nueva reserva para cada hora
                Reserva reserva = new Reserva();
                reserva.setUsuario(usuarioOpt.get());
                reserva.setEspacioDeportivo(espacioOpt.get());
                reserva.setInicioReserva(fecha.atTime(hora, 0));
                reserva.setFinReserva(fecha.atTime(hora + 1, 0));
                reserva.setEstado(Reserva.Estado.confirmada); // O pendiente según tus reglas de negocio
                reserva.setFechaCreacion(LocalDateTime.now());
                reserva.setFechaActualizacion(LocalDateTime.now());

                // Si es reserva de piscina o pista, establecer el número de carril
                if (reservaDTO.getNumeroCarrilPiscina() != null) {
                    reserva.setNumeroCarrilPiscina(reservaDTO.getNumeroCarrilPiscina());
                }
                if (reservaDTO.getNumeroCarrilPista() != null) {
                    reserva.setNumeroCarrilPista(reservaDTO.getNumeroCarrilPista());
                }

                return reserva;
            }).collect(Collectors.toList());

            // Guardar todas las reservas
            List<Reserva> reservasGuardadas = reservaRepository.saveAll(reservasCreadas);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "mensaje", "Reservas creadas correctamente",
                            "reservas", reservasGuardadas
                    ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al crear la reserva: " + e.getMessage()));
        }
    }
}
