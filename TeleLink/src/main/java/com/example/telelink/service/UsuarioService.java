package com.example.telelink.service;

import com.example.telelink.entity.Usuario;
import com.example.telelink.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    public String obtenerNombreUsuario(String correoElectronico) {
        Usuario usuario = usuarioRepository.findByCorreoElectronico(correoElectronico);
        if (usuario != null) {
            return usuario.getNombres() + " " + usuario.getApellidos();
        }
        return "Usuario";
    }
}
