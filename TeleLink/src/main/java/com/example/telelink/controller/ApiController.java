package com.example.telelink.controller;

import com.example.telelink.entity.Reserva;
import com.example.telelink.entity.Usuario;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private ReservaRepository reservaRepository;    
    @GetMapping("reservas/espacio/{id}")
    public List<Map<String, Object>> obtenerReservasPorEspacio(
            @PathVariable Integer id,
            HttpSession session
    ) {
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
    @GetMapping("/verificar-disponibilidad")
    public ResponseEntity<Map<String, Object>> verificarDisponibilidad(
            @RequestParam("espacioId") Integer espacioId,
            @RequestParam("inicio") String inicio,
            @RequestParam("fin") String fin) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Parsear fechas ISO con zona horaria
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
            
            // Verificar conflictos con reservas existentes
            long conflictos = reservaRepository.countActiveReservationConflicts(espacioId, inicioLDT, finLDT);
            
            if (conflictos > 0) {
                response.put("disponible", false);
                response.put("mensaje", "Ya existe una reserva activa en este horario");
            } else {
                response.put("disponible", true);
                response.put("mensaje", "Horario disponible");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("disponible", false);
            response.put("mensaje", "Error al verificar disponibilidad: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
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
}
