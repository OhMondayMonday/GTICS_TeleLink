package com.example.telelink.controller;

import com.example.telelink.entity.EspacioDeportivo;
import com.example.telelink.entity.Usuario;
import com.example.telelink.repository.UsuarioRepository;
import com.example.telelink.service.EspacioDeportivoService;
import com.example.telelink.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/vecino")
public class VecinoController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EspacioDeportivoService espacioService;

    @GetMapping("/index")
    public String index(Model model, Principal principal) {
        Usuario vecino = usuarioRepository.findByCorreoElectronico(principal.getName());
        List<EspacioDeportivo> espacios = espacioService.listarEspaciosOperativos();

        model.addAttribute("vecino", vecino);
        model.addAttribute("espacios", espacios);

        return "vecino/index";
    }

    @Autowired
    private EspacioDeportivoService canchaService;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/vecino-cancha")
    public String mostrarCanchasDisponibles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Double precioMax,
            @RequestParam(required = false) String zona,
            @RequestParam(required = false) Integer rating,
            Model model,
            Principal principal
    ) {
        // Obtener nombre de usuario para el header
        String nombreUsuario = "Usuario";
        if (principal != null) {
            nombreUsuario = usuarioService.obtenerNombreUsuario(principal.getName());
        }
        model.addAttribute("nombreUsuario", nombreUsuario);

        // Paginación (tamaño de página = 6)
        int pageSize = 6;
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize);

        // Aplicar filtros si existen
        Page<EspacioDeportivo> canchasPage;
        if (tipo != null || precioMax != null || zona != null || rating != null) {
            canchasPage = canchaService.buscarCanchasFiltradas(tipo, precioMax, zona, rating, pageRequest);
        } else {
            canchasPage = canchaService.buscarTodasLasCanchas(pageRequest);
        }

        // Agregar atributos al modelo
        model.addAttribute("canchas", canchasPage.getContent());
        model.addAttribute("paginaActual", page);
        model.addAttribute("totalPaginas", canchasPage.getTotalPages());
        model.addAttribute("totalElementos", canchasPage.getTotalElements());

        return "vecino-cancha";
    }
}
