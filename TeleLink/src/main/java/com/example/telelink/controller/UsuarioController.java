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
        Integer userId = 1;
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Almacenar el objeto Usuario en la sesión
        session.setAttribute("currentUser", usuario);

        // Pasar el userId al modelo para la vista
        model.addAttribute("currentUserId", usuario.getUsuarioId());
        model.addAttribute("usuario", usuario);

        return "Vecino/vecino-index";
    }

    @GetMapping("/perfil")
    public String mostrarPerfil(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("currentUser");
        model.addAttribute("usuario", usuario);
        return "Vecino/vecino-perfil";
    }

    @GetMapping("")
    public String listarUsuarios(Model model) {
        List<Usuario> usuarios = usuarioRepository.findAllByOrderByUsuarioIdAsc();
        model.addAttribute("usuarios", usuarios);
        return "lista-usuarios";
    }



}