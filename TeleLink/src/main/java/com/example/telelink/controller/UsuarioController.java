package com.example.telelink.controller;

import com.example.telelink.entity.Usuario;
import com.example.telelink.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/inicio")
    public String mostrarInicio(Model model, HttpSession session) {
        // Buscar usuario ID 1 (usuario por defecto)
        Integer userId = 1;
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Almacenar el objeto Usuario en la sesión
        session.setAttribute("currentUser", usuario);

        // Pasar datos al modelo
        model.addAttribute("usuario", usuario);
        model.addAttribute("activeItem", "inicio");

        return "Vecino/vecino-index";
    }

    @GetMapping("/perfil")
    public String mostrarPerfil(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("currentUser");
        if (usuario == null) {
            return "redirect:/usuarios/inicio";
        }
        model.addAttribute("usuario", usuario);
        model.addAttribute("activeItem", "perfil");
        return "Vecino/vecino-perfil";
    }

    @GetMapping("")
    public String listarUsuarios(Model model) {
        List<Usuario> usuarios = usuarioRepository.findAllByOrderByUsuarioIdAsc();
        model.addAttribute("usuarios", usuarios);
        return "lista-usuarios";
    }

    @GetMapping("/pagos")
    public String mostrarPagos(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("currentUser");
        if (usuario == null) {
            return "redirect:/usuarios/inicio";
        }
        model.addAttribute("usuario", usuario);
        model.addAttribute("activeItem", "pagos");
        return "Vecino/vecino-pago";
    }

    @GetMapping("/reserva")
    public String mostrarReservas(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("currentUser");
        if (usuario == null) {
            return "redirect:/usuarios/inicio";
        }
        model.addAttribute("usuario", usuario);
        model.addAttribute("activeItem", "reservas");
        return "Vecino/vecino-mis-reservas";
    }

    @GetMapping("/cancha")
    public String mostrarCancha(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("currentUser");
        if (usuario == null) {
            return "redirect:/usuarios/inicio";
        }
        model.addAttribute("usuario", usuario);
        model.addAttribute("activeItem", "canchas");
        return "Vecino/vecino-cancha";
    }
    @GetMapping("/ayuda")
    public String mostrarAyuda(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("currentUser");
        if (usuario == null) {
            return "redirect:/usuarios/inicio";
        }
        model.addAttribute("usuario", usuario);
        model.addAttribute("activeItem", "ayuda");
        return "Vecino/vecino-ayuda";
    }

}