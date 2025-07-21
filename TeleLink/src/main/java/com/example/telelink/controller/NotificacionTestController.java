package com.example.telelink.controller;

import com.example.telelink.entity.Usuario;
import com.example.telelink.service.NotificacionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/test")
public class NotificacionTestController {

    @Autowired
    private NotificacionService notificacionService;

    /**
     * Endpoint de prueba para crear notificaciones
     */
    @GetMapping("/crear-notificacion-prueba")
    @ResponseBody
    public String crearNotificacionPrueba(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "Usuario no autenticado";
        }

        // Crear diferentes tipos de notificaciones de prueba
        notificacionService.crearNotificacion(
            usuario.getUsuarioId(),
            "creación",
            "Nueva reserva registrada",
            "Se ha creado una nueva reserva en el espacio deportivo principal",
            "/coordinador/espacios-deportivos"
        );

        notificacionService.crearNotificacion(
            usuario.getUsuarioId(),
            "aviso",
            "Recordatorio importante",
            "Recuerda revisar las asistencias pendientes de esta semana",
            "/coordinador/asistencia"
        );

        notificacionService.crearNotificacion(
            usuario.getUsuarioId(),
            "asignación",
            "Nueva asistencia asignada",
            "Se te ha asignado una nueva asistencia para la actividad de natación",
            "/coordinador/asistencias"
        );

        return "¡Notificaciones de prueba creadas exitosamente!";
    }

    /**
     * Endpoint de prueba para crear una notificación específica
     */
    @GetMapping("/crear-notificacion-cancelacion")
    @ResponseBody
    public String crearNotificacionCancelacion(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "Usuario no autenticado";
        }

        notificacionService.crearNotificacion(
            usuario.getUsuarioId(),
            "cancelación",
            "Reserva cancelada",
            "La reserva del día 15 de enero ha sido cancelada por el usuario",
            "/coordinador/espacios-deportivos"
        );

        return "Notificación de cancelación creada";
    }
}
