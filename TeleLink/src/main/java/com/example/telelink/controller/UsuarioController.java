package com.example.telelink.controller;

import com.example.telelink.entity.Usuario;
import com.example.telelink.repository.UsuarioRepository;
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

    @GetMapping("")
    public String listarUsuarios(Model model) {
        List<Usuario> usuarios = usuarioRepository.findAllByOrderByUsuarioIdAsc();
        model.addAttribute("usuarios", usuarios);
        return "lista-usuarios";
    }

    @GetMapping("/inicio")
    public String mostrarInicio(Model model) {
        return "Coordinador/inicio";
    }
}