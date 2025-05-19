package com.example.telelink.controller;

import com.example.telelink.entity.Reserva;
import com.example.telelink.repository.ReservaRepository;
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
    private ReservaRepository reservaRepository;

    @GetMapping("/espacio/{id}")
    public List<Map<String, Object>> obtenerReservasPorEspacio(@PathVariable Integer id) {
        List<Reserva> reservas = reservaRepository.findByEspacioDeportivo_EspacioDeportivoId(id);

        return reservas.stream().map(reserva -> {
            Map<String, Object> evento = new HashMap<>();
            evento.put("title", "Reservado");
            evento.put("start", reserva.getInicioReserva().toString());
            evento.put("end", reserva.getFinReserva().toString());
            evento.put("color", "#6c757d"); // rojo
            return evento;
        }).collect(Collectors.toList());
    }
}
