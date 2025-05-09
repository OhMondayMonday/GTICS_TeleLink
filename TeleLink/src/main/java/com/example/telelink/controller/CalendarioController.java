package com.example.telelink.controller;


import com.example.telelink.entity.Reserva;
import com.example.telelink.service.ReservaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
public class CalendarioController {

    @Autowired
    private ReservaService reservaService;

    @GetMapping("/vecino/calendario")
    public String mostrarCalendario(Model model) {
        // Obtiene todas las reservas desde el servicio
        List<Reserva> reservas = reservaService.obtenerReservas();

        // Pasamos las reservas al modelo para que se puedan usar en la vista
        model.addAttribute("reservas", reservas);

        return "vecino-calendario"; // Redirige a la vista del calendario
    }

    @PostMapping("/guardarReserva")
    public String guardarReserva(@RequestParam("startDate") String startDate,
                                 @RequestParam("endDate") String endDate,
                                 Principal principal) {
        // Creamos una nueva reserva
        Reserva reserva = new Reserva();
        reserva.setStartDate(startDate);
        reserva.setEndDate(endDate);
        reserva.setUsuario(principal.getName()); // Asume que cada usuario tiene un nombre

        // Guardamos la reserva
        reservaService.guardarReserva(reserva);

        return "redirect:/vecino/reservas"; // Redirige a la página de reservas
    }

    @GetMapping("/vecino/reservas")
    public String verReservas(Model model, Principal principal) {
        // Obtiene las reservas del usuario actual
        List<Reserva> reservas = reservaService.obtenerReservasPorUsuario(principal.getName());
        model.addAttribute("reservas", reservas);

        return "vecino-reservas"; // Redirige a la vista de reservas
    }
}

