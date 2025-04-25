package com.example.telelink.controller;

import com.example.telelink.entity.Asistencia;
import com.example.telelink.repository.AsistenciaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;


@Controller
@RequestMapping("/coordinador")
public class CoordinadorController {

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @GetMapping("/inicio")
    public String mostrarInicio(Model model) {
        return "Coordinador/inicio";
    }

    @GetMapping("/asistencia")
    public String mostrarAsistencia(Model model) {
        return "Coordinador/asistencia";
    }

    @GetMapping("/notificaciones")
    public String mostrarNotificaciones(Model model) {
        return "Coordinador/notificaciones";
    }

    @GetMapping("/observaciones")
    public String mostrarObservaciones(Model model) {
        return "Coordinador/observaciones";
    }

    @GetMapping("/perfil")
    public String mostrarPerfil(Model model) {
        return "Coordinador/perfil";
    }

    @GetMapping("/espacios-deportivos")
    public String mostrarEspaciosDeportivos(Model model) {
        return "Coordinador/espaciosDeportivos";
    }

    @GetMapping("/espacioDetalle")
    public String mostrarDetalleEspacio(Model model) {
        return "Coordinador/espacioDetalle";
    }

    @GetMapping("/observacionDetalle")
    public String mostrarDetalleObservacion(Model model) {
        return "Coordinador/observacionDetalle";
    }

    @GetMapping("/observacionNewForm")
    public String mostrarNewFormObservacion(Model model) {
        return "Coordinador/observacionNewForm";
    }

    @GetMapping("/calendario")
    public ResponseEntity<List<Asistencia>> getAsistenciasParaCalendario(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam int userId) {

        List<Asistencia> asistencias = asistenciaRepository.findForCalendarRange(start, end, userId);
        return ResponseEntity.ok(asistencias);
    }
}
