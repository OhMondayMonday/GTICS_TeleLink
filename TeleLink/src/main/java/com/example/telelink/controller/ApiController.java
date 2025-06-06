package com.example.telelink.controller;

import com.example.telelink.entity.Reserva;
import com.example.telelink.entity.Usuario;
import com.example.telelink.repository.ReservaRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reservas")
public class ApiController {

    @Autowired
    private ReservaRepository reservaRepository;    @GetMapping("/espacio/{id}")
    public List<Map<String, Object>> obtenerReservasPorEspacio(
            @PathVariable Integer id,
            HttpSession session
    ) {
        // Obtener el usuario actual de la sesión
        Usuario usuarioActual = (Usuario) session.getAttribute("usuario");
        Integer usuarioId = usuarioActual != null ? usuarioActual.getUsuarioId() : null;
        
        List<Reserva> reservas = reservaRepository.findByEspacioDeportivo_EspacioDeportivoId(id);

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
            } else {
                evento.put("backgroundColor", "#a0a0a0"); // Gris para reservas ajenas
                evento.put("borderColor", "#808080");
                evento.put("esPropia", false);
            }
            
            return evento;
        }).collect(Collectors.toList());
    }
}
