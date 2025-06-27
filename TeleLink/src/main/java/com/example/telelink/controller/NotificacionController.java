package com.example.telelink.controller;

import com.example.telelink.entity.Notificacion;
import com.example.telelink.entity.Usuario;
import com.example.telelink.service.NotificacionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/notificaciones")
public class NotificacionController {

    @Autowired
    private NotificacionService notificacionService;

    /**
     * Endpoint para obtener las últimas 5 notificaciones (dropdown)
     */
    @GetMapping("/api/latest")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerUltimasNotificaciones(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return ResponseEntity.status(401).build();
        }

        List<Notificacion> notificaciones = notificacionService.obtenerUltimas5Notificaciones(usuario.getUsuarioId());
        long conteoNoLeidas = notificacionService.contarNotificacionesNoLeidas(usuario.getUsuarioId());

        Map<String, Object> response = new HashMap<>();
        response.put("notificaciones", notificaciones);
        response.put("conteoNoLeidas", conteoNoLeidas);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para obtener solo el conteo de notificaciones no leídas
     */
    @GetMapping("/api/unread-count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerConteoNoLeidas(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return ResponseEntity.status(401).build();
        }

        long conteoNoLeidas = notificacionService.contarNotificacionesNoLeidas(usuario.getUsuarioId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("conteoNoLeidas", conteoNoLeidas);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para marcar una notificación como leída y redirigir
     */
    @PostMapping("/marcar-leida/{notificacionId}")
    public String marcarComoLeidaYRedirigir(@PathVariable Integer notificacionId, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        Optional<Notificacion> notificacionOpt = notificacionService.obtenerNotificacionPorId(notificacionId);
        if (notificacionOpt.isPresent()) {
            Notificacion notificacion = notificacionOpt.get();
            
            // Verificar que la notificación pertenece al usuario logueado
            if (notificacion.getUsuario().getUsuarioId().equals(usuario.getUsuarioId())) {
                notificacionService.marcarComoLeida(notificacionId);
                
                // Redirigir a la URL especificada o al dashboard por defecto
                String urlRedireccion = notificacion.getUrlRedireccion();
                if (urlRedireccion != null && !urlRedireccion.isEmpty()) {
                    return "redirect:" + urlRedireccion;
                }
            }
        }
        
        return "redirect:/coordinador/dashboard";
    }

    /**
     * Endpoint para marcar todas las notificaciones como leídas
     */
    @PostMapping("/api/marcar-todas-leidas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> marcarTodasComoLeidas(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return ResponseEntity.status(401).build();
        }

        notificacionService.marcarTodasComoLeidas(usuario.getUsuarioId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Todas las notificaciones han sido marcadas como leídas");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Página de notificaciones completa
     */
    @GetMapping("/lista")
    public String listaNotificaciones(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            HttpSession session,
            Model model) {
        
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        Pageable pageable = PageRequest.of(page, size);
        
        // Valores por defecto
        LocalDate fechaInicioDefault = fechaInicio != null ? fechaInicio : LocalDate.now().minusDays(30);
        LocalDate fechaFinDefault = fechaFin != null ? fechaFin : LocalDate.now();
        String estadoDefault = estado != null && !estado.isEmpty() ? estado : "";
        
        // Convertir estado string a enum
        Notificacion.Estado estadoEnum = null;
        if (estadoDefault != null && !estadoDefault.isEmpty()) {
            try {
                estadoEnum = Notificacion.Estado.valueOf(estadoDefault);
            } catch (IllegalArgumentException e) {
                // Estado inválido, ignorar
            }
        }

        Page<Notificacion> notificaciones;
        
        // Convertir LocalDate a LocalDateTime para el servicio
        LocalDateTime fechaInicioDateTime = fechaInicioDefault.atStartOfDay();
        LocalDateTime fechaFinDateTime = fechaFinDefault.atTime(23, 59, 59);
        
        // Usar siempre filtros con las fechas por defecto
        notificaciones = notificacionService.obtenerNotificacionesConFiltros(
            usuario.getUsuarioId(), estadoEnum, null, fechaInicioDateTime, fechaFinDateTime, pageable);

        model.addAttribute("usuario", usuario);
        model.addAttribute("notificaciones", notificaciones);
        model.addAttribute("estadoSeleccionado", estadoDefault);
        model.addAttribute("fechaInicio", fechaInicioDefault);
        model.addAttribute("fechaFin", fechaFinDefault);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notificaciones.getTotalPages());
        model.addAttribute("totalElements", notificaciones.getTotalElements());

        return "Coordinador/notificaciones";
    }

    /**
     * Endpoints específicos para ADMIN
     */
    
    /**
     * Página de notificaciones completa para ADMIN
     */
    @GetMapping("/admin/lista")
    public String listaNotificacionesAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            HttpSession session,
            Model model) {
        
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        Pageable pageable = PageRequest.of(page, size);
        
        // Valores por defecto
        LocalDate fechaInicioDefault = fechaInicio != null ? fechaInicio : LocalDate.now().minusDays(30);
        LocalDate fechaFinDefault = fechaFin != null ? fechaFin : LocalDate.now();
        String estadoDefault = estado != null && !estado.isEmpty() ? estado : "";
        
        // Convertir estado string a enum
        Notificacion.Estado estadoEnum = null;
        if (estadoDefault != null && !estadoDefault.isEmpty()) {
            try {
                estadoEnum = Notificacion.Estado.valueOf(estadoDefault);
            } catch (IllegalArgumentException e) {
                // Estado inválido, ignorar
            }
        }

        Page<Notificacion> notificaciones;
        
        // Convertir LocalDate a LocalDateTime para el servicio
        LocalDateTime fechaInicioDateTime = fechaInicioDefault.atStartOfDay();
        LocalDateTime fechaFinDateTime = fechaFinDefault.atTime(23, 59, 59);
        
        // Usar siempre filtros con las fechas por defecto
        notificaciones = notificacionService.obtenerNotificacionesConFiltros(
            usuario.getUsuarioId(), estadoEnum, null, fechaInicioDateTime, fechaFinDateTime, pageable);

        model.addAttribute("usuario", usuario);
        model.addAttribute("notificaciones", notificaciones);
        model.addAttribute("estadoSeleccionado", estadoDefault);
        model.addAttribute("fechaInicio", fechaInicioDefault);
        model.addAttribute("fechaFin", fechaFinDefault);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notificaciones.getTotalPages());
        model.addAttribute("totalElements", notificaciones.getTotalElements());

        return "admin/notificaciones";
    }

    /**
     * Endpoint para marcar una notificación como leída y redirigir - ADMIN
     */
    @PostMapping("/admin/marcar-leida/{notificacionId}")
    public String marcarComoLeidaYRedirigirAdmin(@PathVariable Integer notificacionId, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        Optional<Notificacion> notificacionOpt = notificacionService.obtenerNotificacionPorId(notificacionId);
        if (notificacionOpt.isPresent()) {
            Notificacion notificacion = notificacionOpt.get();
            
            // Verificar que la notificación pertenece al usuario logueado
            if (notificacion.getUsuario().getUsuarioId().equals(usuario.getUsuarioId())) {
                notificacionService.marcarComoLeida(notificacionId);
                
                // Redirigir a la URL especificada o al dashboard por defecto
                String urlRedireccion = notificacion.getUrlRedireccion();
                if (urlRedireccion != null && !urlRedireccion.isEmpty()) {
                    return "redirect:" + urlRedireccion;
                }
            }
        }
        
        return "redirect:/admin/dashboard";
    }

    /**
     * Endpoint para obtener información de icono y color por tipo de notificación
     */
    @GetMapping("/api/tipo-info/{tipo}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> obtenerInfoTipo(@PathVariable String tipo) {
        Map<String, String> info = obtenerIconoYColor(tipo);
        return ResponseEntity.ok(info);
    }

    /**
     * Método auxiliar para mapear tipo de notificación a icono y color
     */
    private Map<String, String> obtenerIconoYColor(String tipoNotificacion) {
        Map<String, String> info = new HashMap<>();
        
        switch (tipoNotificacion.toLowerCase()) {
            case "creación":
                info.put("icono", "fas fa-plus-circle");
                info.put("color", "#28a745"); // Verde
                break;
            case "aviso":
                info.put("icono", "fas fa-exclamation-triangle");
                info.put("color", "#ffc107"); // Amarillo
                break;
            case "cancelación":
                info.put("icono", "fas fa-times-circle");
                info.put("color", "#dc3545"); // Rojo
                break;
            case "actualización":
                info.put("icono", "fas fa-edit");
                info.put("color", "#17a2b8"); // Azul cian
                break;
            case "aprobación":
                info.put("icono", "fas fa-check-circle");
                info.put("color", "#007bff"); // Azul
                break;
            case "asignación":
                info.put("icono", "fas fa-user-plus");
                info.put("color", "#6610f2"); // Púrpura
                break;
            default:
                info.put("icono", "fas fa-bell");
                info.put("color", "#6c757d"); // Gris
                break;
        }
        
        return info;
    }
}
