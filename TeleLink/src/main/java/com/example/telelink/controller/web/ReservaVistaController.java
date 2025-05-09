package com.example.telelink.controller.web;

import com.example.telelink.repository.EspacioDeportivoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/reservas")
public class ReservaVistaController {

    @Autowired
    private EspacioDeportivoRepository espacioDeportivoRepository;

    @GetMapping("/calendario")
    public String mostrarCalendario(Model model) {
        model.addAttribute("espacios", espacioDeportivoRepository.findAll());
        return "reservas/calendario";
    }

    @GetMapping("/crear")
    public String mostrarFormularioReserva(Model model) {
        model.addAttribute("espacios", espacioDeportivoRepository.findAll());
        return "reservas/crearReserva";
    }
}

