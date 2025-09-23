package com.example.telelink.service;

import com.example.telelink.entity.Aviso;
import com.example.telelink.repository.AvisoRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class AvisoService {

    @Autowired
    private AvisoRepository avisoRepo;

    public void activarAviso(Integer id) {
        Aviso avisoAActivar = avisoRepo.findById(id).orElseThrow();

        // 1. Validar que no sea el default
        if (avisoAActivar.esEstadoDefault()) {
            throw new IllegalStateException("El aviso default no puede activarse/desactivarse");
        }

        // 2. Desactivar cualquier otro aviso activo (no default)
        avisoRepo.findByEstadoAviso("activo").stream()
                .filter(a -> !a.esEstadoDefault())
                .findFirst()
                .ifPresent(avisoActivo -> {
                    avisoActivo.setEstadoAviso("disponible");
                    avisoRepo.save(avisoActivo);
                });

        // 3. Activar el nuevo aviso
        avisoAActivar.setEstadoAviso("activo");
        avisoRepo.save(avisoAActivar);
    }

    public void desactivarAviso(Integer id) {
        Aviso aviso = avisoRepo.findById(id).orElseThrow();

        // 1. Validar que no sea el default
        if (aviso.esEstadoDefault()) {
            throw new IllegalStateException("El aviso default no puede modificarse");
        }

        // 2. Solo desactivar si está activo
        if ("activo".equals(aviso.getEstadoAviso())) {
            aviso.setEstadoAviso("disponible");
            avisoRepo.save(aviso);
        }
    }
}