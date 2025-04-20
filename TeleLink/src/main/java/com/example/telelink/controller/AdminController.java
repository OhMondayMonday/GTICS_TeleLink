package com.example.telelink.controller;

import com.example.telelink.entity.EstablecimientoDeportivo;
import com.example.telelink.repository.EstablecimientoDeportivoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private EstablecimientoDeportivoRepository establecimientoDeportivoRepository;

    @GetMapping("establecimientos")
    public String listarEstablecimientos(Model model) {
        List<EstablecimientoDeportivo> establecimientosList = establecimientoDeportivoRepository.findAll();

        model.addAttribute("establecimientos", establecimientosList);
        return "admin/establecimientoList";
    }

    @GetMapping("establecimientos/nuevo")
    public String crearEstablecimiento(Model model) {
        model.addAttribute("establecimiento", new EstablecimientoDeportivo());
        return "admin/establecimientoForm";
    }

    @PostMapping("establecimientos/guardar")
    public String guardarEstablecimiento(EstablecimientoDeportivo establecimiento) {
        // Lógica para guardar el establecimiento en la base de datos
        establecimientoDeportivoRepository.save(establecimiento);
        return "redirect:/admin/establecimientos"; // Redirige a la lista de establecimientos
    }

}
