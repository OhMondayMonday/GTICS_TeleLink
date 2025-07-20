package com.example.telelink.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

@ControllerAdvice
public class GlobalModelAttributes {
    @ModelAttribute
    public void addRoleToModel(Authentication authentication, Model model) {
        String rol = "vecino";
        if (authentication != null) {
            rol = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst().orElse("vecino");
        }
        model.addAttribute("rol", rol);
    }
}
