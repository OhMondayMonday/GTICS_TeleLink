package com.example.telelink.controller;

import com.example.telelink.entity.EspacioDeportivo;
import com.example.telelink.repository.EspacioDeportivoRepository;
import com.example.telelink.service.EspacioDeportivoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/espacios")
public class EspacioDeportivoController {
    @Autowired
    private EspacioDeportivoService espacioDeportivoService;

    @GetMapping
    public String listarEspaciosDeportivos(Model model) {
        List<EspacioDeportivo> espacios = espacioDeportivoService.findAll();
        model.addAttribute("espacios", espacios);
        return "vecino-espacios-deportivos";
    }

    @GetMapping("/{id}")
    public String verEspacioDeportivo(@PathVariable Integer id, Model model) {
        Optional<EspacioDeportivo> espacio = espacioDeportivoService.findById(id);

        if (espacio.isPresent()) {
            model.addAttribute("espacio", espacio.get());

            // Determinar la vista según el tipo de servicio deportivo
            String tipoServicio = espacio.get().getServicioDeportivo().getServicioDeportivo().toLowerCase();

            if (tipoServicio.contains("piscina")) {
                return "vecino-piscina";
            } else if (tipoServicio.contains("gimnasio")) {
                return "vecino-gimnasio";
            } else if (tipoServicio.contains("futbol") || tipoServicio.contains("fútbol")) {
                return "vecino-futbol";
            } else if (tipoServicio.contains("atletismo")) {
                return "vecino-atletismo";
            } else {
                return "vecino-espacio-generico";
            }
        }

        return "redirect:/espacios-deportivos";
    }

    @GetMapping("/por-servicio/{servicioId}")
    public String listarPorServicio(@PathVariable Integer servicioId, Model model) {
        List<EspacioDeportivo> espacios = espacioDeportivoService.findByServicioDeportivoId(servicioId);
        model.addAttribute("espacios", espacios);
        return "vecino-espacios-deportivos";
    }
}
