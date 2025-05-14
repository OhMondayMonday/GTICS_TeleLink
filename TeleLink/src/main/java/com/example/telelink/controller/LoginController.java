package com.example.telelink.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.Map;

@Controller
public class LoginController {

    @GetMapping("/openLoginWindow")
    public String loginWindow() {
        // Verificar si el usuario está autenticado
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            // Obtener el rol del usuario
            String rol = authentication.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .findFirst()
                    .orElse("vecino");

            // Mapa de redirecciones según el rol
            Map<String, String> redirectMap = new HashMap<>();
            redirectMap.put("superadmin", "/superadmin/inicio");
            redirectMap.put("administrador", "/admin/dashboard");
            redirectMap.put("coordinador", "/coordinador/inicio");
            redirectMap.put("vecino", "/usuarios/");

            // Redirigir a la página principal del rol
            return "redirect:" + redirectMap.getOrDefault(rol, "/usuarios/");
        }

        // Si no está autenticado, mostrar la página de login
        return "loginWindow";
    }

    @GetMapping("/acceso-denegado")
    public String accesoDenegado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String rol = authentication.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .findFirst()
                    .orElse("vecino");

            Map<String, String> redirectMap = new HashMap<>();
            redirectMap.put("superadmin", "/superadmin/inicio");
            redirectMap.put("administrador", "/admin/dashboard");
            redirectMap.put("coordinador", "/coordinador/inicio");
            redirectMap.put("vecino", "/usuarios/");

            String redirectUrl = redirectMap.getOrDefault(rol, "/usuarios/");
            return "redirect:" + redirectUrl;
        }
        return "redirect:/openLoginWindow";
    }


    @GetMapping("/register")
    public String registerForm() {
        return "registro";
    }

}