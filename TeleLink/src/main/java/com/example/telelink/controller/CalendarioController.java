package com.example.telelink.controller;


import com.example.telelink.entity.Reserva;
import com.example.telelink.entity.Usuario;
import com.example.telelink.repository.UsuarioRepository;
import com.example.telelink.service.ReservaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class CalendarioController {

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/vecino/calendario")
    public String mostrarCalendario(Model model) {
        // Obtiene todas las reservas desde el servicio
        List<Reserva> reservas = reservaService.obtenerReservas();

        // Pasamos las reservas al modelo para que se puedan usar en la vista
        model.addAttribute("reservas", reservas);

        return "vecino-calendario"; // Redirige a la vista del calendario
    }

    /*@PostMapping("/guardarReserva")
    public String guardarReserva(@RequestParam("startDate") String startDate,
                                 @RequestParam("endDate") String endDate,
                                 Principal principal) {
        // Creamos una nueva reserva
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        LocalDateTime start = LocalDateTime.parse(startDate, formatter);
        LocalDateTime end = LocalDateTime.parse(endDate, formatter);

        Usuario usuario = usuarioRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Reserva reserva = new Reserva();
        reserva.setInicioReserva(start);
        reserva.setFinReserva(end);
        reserva.setUsuario(usuario); // Asume que cada usuario tiene un nombre

        // Guardamos la reserva
        reservaService.guardarReserva(reserva);

        return "redirect:/vecino/reservas"; // Redirige a la página de reservas
    }*/

    @GetMapping("/vecino/reservas")
    public String verReservas(Model model, Principal principal) {
        // Obtiene las reservas del usuario actual
        List<Reserva> reservas = reservaService.obtenerReservasPorUsuario(principal.getName());
        model.addAttribute("reservas", reservas);

        return "vecino-reservas"; // Redirige a la vista de reservas
    }
}

