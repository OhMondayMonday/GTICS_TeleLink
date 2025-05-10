package com.example.telelink.controller;

import com.example.telelink.entity.EspacioDeportivo;
import com.example.telelink.repository.EspacioDeportivoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/espacios")
public class EspacioDeportivoController {

    @Autowired
    private EspacioDeportivoRepository espacioDeportivoRepository;

    @GetMapping("/espacios/{id}")
    public String mostrarDetalleEspacio(@PathVariable("id") Integer id, Model model) {
        Optional<EspacioDeportivo> optEspacio = espacioDeportivoRepository.findById(id);

        if (optEspacio.isPresent()) {
            model.addAttribute("espacio", optEspacio.get());

            // Aquí puedes añadir más atributos según el tipo de servicio
            String vistaDestino = "vecino/vecino-";

            EspacioDeportivo espacio = optEspacio.get();
            Integer servicioId = espacio.getServicioDeportivo().getServicioDeportivoId();

            // Determinar qué vista mostrar según el tipo de servicio
            switch (servicioId) {
                case 1:
                    vistaDestino += "futbol";
                    break;
                case 2:
                    vistaDestino += "piscina";
                    break;
                case 3:
                    vistaDestino += "multiple";
                    break;
                case 4:
                    vistaDestino += "gym";
                    break;
                default:
                    vistaDestino += "espacio";
            }

            return vistaDestino;
        } else {
            return "redirect:/vecino/vecino-mis-reservas";
        }
    }

}
