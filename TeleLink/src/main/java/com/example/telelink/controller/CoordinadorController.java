package com.example.telelink.controller;

import com.example.telelink.entity.*;
import com.example.telelink.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Controller
@RequestMapping("/coordinador")
public class CoordinadorController {

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private EstablecimientoDeportivoRepository establecimientoDeportivoRepository;

    @Autowired
    private ServicioDeportivoRepository servicioDeportivoRepository;

    @Autowired
    private EspacioDeportivoRepository espacioDeportivoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private AvisoRepository avisoRepository;

    @GetMapping("/inicio")
    public String mostrarInicio(Model model, HttpSession session) {
        // Simulamos el usuario logueado con userId = 6 (reemplazar con autenticación real en producción)
        Integer userId = 6;
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Almacenar el objeto Usuario en la sesión
        session.setAttribute("currentUser", usuario);

        // Obtener el aviso más reciente
        Aviso ultimoAviso = avisoRepository.findLatestAviso();

        // Pasar datos al modelo
        model.addAttribute("currentUserId", usuario.getUsuarioId());
        model.addAttribute("usuario", usuario);
        model.addAttribute("ultimoAviso", ultimoAviso);

        return "Coordinador/inicio";
    }

    @GetMapping("/asistencia")
    public String mostrarAsistencia(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("currentUser");
        model.addAttribute("usuario", usuario);
        return "Coordinador/asistencia";
    }

    @GetMapping("/notificaciones")
    public String mostrarNotificaciones(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("currentUser");
        model.addAttribute("usuario", usuario);
        return "Coordinador/notificaciones";
    }

    @GetMapping("/observaciones")
    public String mostrarObservaciones(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("currentUser");
        model.addAttribute("usuario", usuario);
        return "Coordinador/observaciones";
    }

    @GetMapping("/perfil")
    public String mostrarPerfil(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("currentUser");
        model.addAttribute("usuario", usuario);
        return "Coordinador/perfil";
    }

    @GetMapping("/editar-perfil")
    public String mostrarEditarPerfil(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("currentUser");
        model.addAttribute("usuario", usuario);
        return "Coordinador/editarPerfil";
    }

    @PostMapping("/actualizar-perfil")
    public String actualizarPerfil(@ModelAttribute("usuario") Usuario usuarioActualizado, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("currentUser");

        // Actualizar solo los campos permitidos (teléfono y foto)
        usuario.setTelefono(usuarioActualizado.getTelefono());
        // Por ahora, usamos una URL fija como ejemplo, ya que no se implementa la carga de imágenes
        usuario.setFotoPerfilUrl(usuarioActualizado.getFotoPerfilUrl() != null && !usuarioActualizado.getFotoPerfilUrl().isEmpty()
                ? usuarioActualizado.getFotoPerfilUrl()
                : "https://st3.depositphotos.com/12985790/15794/i/450/depositphotos_157947226-stock-photo-man-looking-at-camera.jpg");

        // Guardar los cambios en la base de datos
        usuarioRepository.save(usuario);

        // Actualizar el objeto en la sesión
        session.setAttribute("currentUser", usuario);

        return "redirect:/coordinador/perfil";
    }

    @GetMapping("/espacios-deportivos")
    public String mostrarEspaciosDeportivos(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("currentUser");
        model.addAttribute("usuario", usuario);
        return "Coordinador/espaciosDeportivos";
    }

    @GetMapping("/espacioDetalle")
    public String mostrarDetalleEspacio(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("currentUser");
        model.addAttribute("usuario", usuario);
        return "Coordinador/espacioDetalle";
    }

    @GetMapping("/observacionDetalle")
    public String mostrarDetalleObservacion(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("currentUser");
        model.addAttribute("usuario", usuario);
        return "Coordinador/observacionDetalle";
    }

    @GetMapping("/observacionNewForm")
    public String mostrarNewFormObservacion(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("currentUser");
        model.addAttribute("usuario", usuario);
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

    // Endpoint para obtener estadísticas de asistencias (opcional, si prefieres AJAX para el donut chart)
    @GetMapping("/estadisticas-asistencias")
    public ResponseEntity<Map<String, Long>> getEstadisticasAsistencias(@RequestParam int userId) {
        Map<String, Long> estadisticas = asistenciaRepository.countByEstadoEntrada(userId);
        return ResponseEntity.ok(estadisticas);
    }

    /*
    @GetMapping("/calendarioCoordi")
    public String mostrarCalendario(Model model) {
        return "Coordinador/calendar";
    }

    // Obtener todos los establecimientos deportivos
    @GetMapping("/establecimientos")
    public ResponseEntity<List<EstablecimientoDeportivo>> getEstablecimientos() {
        List<EstablecimientoDeportivo> establecimientos = establecimientoDeportivoRepository.findAll();
        return ResponseEntity.ok(establecimientos);
    }

    // Obtener servicios deportivos por establecimiento
    @GetMapping("/servicios-por-establecimiento")
    public ResponseEntity<List<ServicioDeportivo>> getServiciosPorEstablecimiento(
            @RequestParam("establecimientoId") Integer establecimientoId) {
        List<ServicioDeportivo> servicios = servicioDeportivoRepository.findByEstablecimientoDeportivoId(establecimientoId);
        return ResponseEntity.ok(servicios);
    }

    // Obtener espacios deportivos por establecimiento y servicio
    @GetMapping("/espacios-por-servicio")
    public ResponseEntity<List<EspacioDeportivo>> getEspaciosPorServicio(
            @RequestParam("establecimientoId") Integer establecimientoId,
            @RequestParam("servicioId") Integer servicioId) {
        List<EspacioDeportivo> espacios = espacioDeportivoRepository.findByEstablecimientoAndServicio(establecimientoId, servicioId);
        return ResponseEntity.ok(espacios);
    }

    // Crear una nueva asistencia
    @PostMapping("/asistencia")
    public ResponseEntity<Asistencia> crearAsistencia(@RequestBody AsistenciaDTO asistenciaDTO) {
        Asistencia asistencia = new Asistencia();

        // Configurar coordinador (fijo a userId=11)
        final int COORDINADOR_ID = 11;
        Usuario coordinador = usuarioRepository.findById(COORDINADOR_ID)
                .orElseThrow(() -> new IllegalArgumentException("Coordinador no encontrado"));
        asistencia.setCoordinador(coordinador);

        // Configurar administrador (fijo a adminId=1 para pruebas)
        Usuario administrador = usuarioRepository.findById(1)
                .orElseThrow(() -> new IllegalArgumentException("Administrador no encontrado"));
        asistencia.setAdministrador(administrador);

        // Configurar espacio deportivo
        EspacioDeportivo espacio = espacioDeportivoRepository.findById(asistenciaDTO.getEspacioDeportivoId())
                .orElseThrow(() -> new IllegalArgumentException("Espacio deportivo no encontrado"));
        asistencia.setEspacioDeportivo(espacio);

        // Validar horarios contra el establecimiento
        EstablecimientoDeportivo establecimiento = espacio.getEstablecimientoDeportivo();
        LocalDateTime horarioEntrada = asistenciaDTO.getHorarioEntrada();
        LocalDateTime horarioSalida = asistenciaDTO.getHorarioSalida();

        // Validar que la hora de entrada no sea menor a la hora de apertura
        if (horarioEntrada.toLocalTime().isBefore(establecimiento.getHorarioApertura())) {
            throw new IllegalArgumentException("La hora de entrada no puede ser menor a la hora de apertura del establecimiento");
        }

        // Validar que la hora de salida no sea mayor a la hora de cierre
        if (horarioSalida.toLocalTime().isAfter(establecimiento.getHorarioCierre())) {
            throw new IllegalArgumentException("La hora de salida no puede ser mayor a la hora de cierre del establecimiento");
        }

        // Validar que la hora de entrada sea menor a la hora de salida
        if (!horarioEntrada.isBefore(horarioSalida)) {
            throw new IllegalArgumentException("La hora de entrada debe ser menor a la hora de salida");
        }

        // Validar que no haya solapamiento con otras asistencias del coordinador
        List<Asistencia> asistenciasSolapadas = asistenciaRepository.findOverlappingAsistencias(
                COORDINADOR_ID, horarioEntrada, horarioSalida);
        if (!asistenciasSolapadas.isEmpty()) {
            throw new IllegalArgumentException("El coordinador ya tiene una asistencia programada que se cruza con este horario");
        }

        // Configurar asistencia
        asistencia.setHorarioEntrada(horarioEntrada);
        asistencia.setHorarioSalida(horarioSalida);
        asistencia.setEstadoEntrada(Asistencia.EstadoEntrada.pendiente);
        asistencia.setEstadoSalida(Asistencia.EstadoSalida.pendiente);
        asistencia.setRegistroEntrada(null);
        asistencia.setRegistroSalida(null);
        asistencia.setGeolocalizacion(null);
        asistencia.setObservacionAsistencia(asistenciaDTO.getObservacionAsistencia());
        asistencia.setFechaCreacion(LocalDateTime.now());

        // Guardar asistencia
        Asistencia savedAsistencia = asistenciaRepository.save(asistencia);
        return ResponseEntity.ok(savedAsistencia);
    }

    // DTO para recibir datos del frontend
    public static class AsistenciaDTO {
        private Integer espacioDeportivoId;
        private LocalDateTime horarioEntrada;
        private LocalDateTime horarioSalida;
        private String observacionAsistencia;

        public Integer getEspacioDeportivoId() { return espacioDeportivoId; }
        public void setEspacioDeportivoId(Integer espacioDeportivoId) { this.espacioDeportivoId = espacioDeportivoId; }
        public LocalDateTime getHorarioEntrada() { return horarioEntrada; }
        public void setHorarioEntrada(LocalDateTime horarioEntrada) { this.horarioEntrada = horarioEntrada; }
        public LocalDateTime getHorarioSalida() { return horarioSalida; }
        public void setHorarioSalida(LocalDateTime horarioSalida) { this.horarioSalida = horarioSalida; }
        public String getObservacionAsistencia() { return observacionAsistencia; }
        public void setObservacionAsistencia(String observacionAsistencia) { this.observacionAsistencia = observacionAsistencia; }
    }
    */
}
