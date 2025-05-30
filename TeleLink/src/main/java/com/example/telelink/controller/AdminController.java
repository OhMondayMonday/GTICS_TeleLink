package com.example.telelink.controller;

import com.example.telelink.dto.EventoCalendarioDTO;
import com.example.telelink.dto.admin.CantidadReservasPorDiaDto;
import com.example.telelink.entity.*;
import com.example.telelink.repository.*;
import com.example.telelink.service.S3Service;
import com.example.telelink.service.CalendarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private EstablecimientoDeportivoRepository establecimientoDeportivoRepository;

    @Autowired
    private AvisoRepository avisoRepository;

    @Autowired
    private EspacioDeportivoRepository espacioDeportivoRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private ServicioDeportivoRepository servicioDeportivoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private ObservacionRepository observacionRepository;

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private TipoNotificacionRepository tipoNotificacionRepository;
    @Autowired
    private MantenimientoRepository mantenimientoRepository;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private CalendarioService calendarioService;

    @GetMapping("/calendario")
    public ResponseEntity<List<Asistencia>> getAsistenciasParaCalendario(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam int userId) {
        List<Asistencia> asistencias = asistenciaRepository.findForCalendarRange(start, end, userId);
        return ResponseEntity.ok(asistencias);
    }

    @GetMapping("/asistencias/nueva")
    public String nuevaAsistencia(@RequestParam("coordinadorId") Integer coordinadorId, Model model) {
        model.addAttribute("coordinadorId", coordinadorId);
        model.addAttribute("establecimientos", establecimientoDeportivoRepository.findAll());
        return "admin/nuevaAsistencia";
    }

    @GetMapping("/espacios-por-establecimiento")
    public ResponseEntity<List<EspacioDeportivo>> getEspaciosPorEstablecimiento(@RequestParam("establecimientoId") Integer establecimientoId) {
        List<EspacioDeportivo> espacios = espacioDeportivoRepository.findAll().stream()
                .filter(e -> e.getEstablecimientoDeportivo().getEstablecimientoDeportivoId().equals(establecimientoId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(espacios);
    }

    @GetMapping("/espacio-horario")
    public ResponseEntity<?> getEspacioHorario(@RequestParam("espacioId") Integer espacioId) {
        Optional<EspacioDeportivo> optEspacio = espacioDeportivoRepository.findById(espacioId);
        if (optEspacio.isPresent()) {
            EspacioDeportivo espacio = optEspacio.get();
            return ResponseEntity.ok(new HorarioResponse(espacio.getHorarioApertura().toString(), espacio.getHorarioCierre().toString()));
        }
        return ResponseEntity.badRequest().body("Espacio no encontrado");
    }

    @PostMapping("/asistencias/guardar")
    public String guardarAsistencia(
            @RequestParam("coordinadorId") Integer coordinadorId,
            @RequestParam("administradorId") Integer administradorId,
            @RequestParam("espacioDeportivoId") Integer espacioDeportivoId,
            @RequestParam("establecimientoDeportivoId") Integer establecimientoDeportivoId,
            @RequestParam(value = "fecha", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fecha,
            @RequestParam(value = "horarioEntrada", required = false) String horarioEntradaStr,
            @RequestParam(value = "horarioSalida", required = false) String horarioSalidaStr,
            RedirectAttributes attr,
            Model model) {

        // Initialize error map
        Map<String, String> errors = new HashMap<>();

        // Validate required fields
        if (coordinadorId == null) {
            errors.put("coordinadorId", "El ID del coordinador es requerido");
        }
        if (administradorId == null) {
            errors.put("administradorId", "El ID del administrador es requerido");
        }
        if (establecimientoDeportivoId == null) {
            errors.put("establecimientoDeportivoId", "Debe seleccionar un establecimiento deportivo");
        }
        if (espacioDeportivoId == null) {
            errors.put("espacioDeportivoId", "Debe seleccionar un espacio deportivo");
        }
        if (fecha == null) {
            errors.put("fecha", "La fecha es requerida");
        }
        if (horarioEntradaStr == null || horarioEntradaStr.trim().isEmpty()) {
            errors.put("horarioEntrada", "El horario de entrada es requerido");
        }
        if (horarioSalidaStr == null || horarioSalidaStr.trim().isEmpty()) {
            errors.put("horarioSalida", "El horario de salida es requerido");
        }

        // Validate date is tomorrow or later
        if (fecha != null && fecha.isBefore(LocalDate.now().plusDays(1))) {
            errors.put("fecha", "La fecha debe ser a partir de mañana");
        }

        // Parse times and validate format
        LocalTime horarioEntradaTime = null;
        LocalTime horarioSalidaTime = null;
        LocalDateTime horarioEntrada = null;
        LocalDateTime horarioSalida = null;

        if (horarioEntradaStr != null && !horarioEntradaStr.trim().isEmpty()) {
            try {
                horarioEntradaTime = LocalTime.parse(horarioEntradaStr);
            } catch (DateTimeParseException e) {
                errors.put("horarioEntrada", "Formato de horario de entrada inválido");
            }
        }
        if (horarioSalidaStr != null && !horarioSalidaStr.trim().isEmpty()) {
            try {
                horarioSalidaTime = LocalTime.parse(horarioSalidaStr);
            } catch (DateTimeParseException e) {
                errors.put("horarioSalida", "Formato de horario de salida inválido");
            }
        }

        // If there are errors, return to form
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            model.addAttribute("coordinadorId", coordinadorId);
            model.addAttribute("establecimientos", establecimientoDeportivoRepository.findAll());
            return "admin/nuevaAsistencia";
        }

        // Combine date and times
        horarioEntrada = LocalDateTime.of(fecha, horarioEntradaTime);
        horarioSalida = LocalDateTime.of(fecha, horarioSalidaTime);

        // Validate time range
        if (!horarioSalida.isAfter(horarioEntrada)) {
            errors.put("horarioSalida", "El horario de salida debe ser posterior al de entrada");
            model.addAttribute("errors", errors);
            model.addAttribute("coordinadorId", coordinadorId);
            model.addAttribute("establecimientos", establecimientoDeportivoRepository.findAll());
            return "admin/nuevaAsistencia";
        }

        // Fetch entities
        Optional<Usuario> optCoordinador = usuarioRepository.findById(coordinadorId);
        Optional<Usuario> optAdministrador = usuarioRepository.findById(administradorId);
        Optional<EspacioDeportivo> optEspacio = espacioDeportivoRepository.findById(espacioDeportivoId);

        if (!optCoordinador.isPresent()) {
            errors.put("coordinadorId", "Coordinador no encontrado");
        }
        if (!optAdministrador.isPresent()) {
            errors.put("administradorId", "Administrador no encontrado");
        }
        if (!optEspacio.isPresent()) {
            errors.put("espacioDeportivoId", "Espacio deportivo no encontrado");
        }

        // Validate space hours
        if (optEspacio.isPresent()) {
            EspacioDeportivo espacio = optEspacio.get();
            if (horarioEntradaTime.isBefore(espacio.getHorarioApertura()) || horarioEntradaTime.isAfter(espacio.getHorarioCierre())) {
                errors.put("horarioEntrada", "El horario de entrada debe estar dentro del horario del espacio (" + espacio.getHorarioApertura() + " - " + espacio.getHorarioCierre() + ")");
            }
            if (horarioSalidaTime.isBefore(espacio.getHorarioApertura()) || horarioSalidaTime.isAfter(espacio.getHorarioCierre())) {
                errors.put("horarioSalida", "El horario de salida debe estar dentro del horario del espacio (" + espacio.getHorarioApertura() + " - " + espacio.getHorarioCierre() + ")");
            }
        }

        // If there are errors, return to form
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            model.addAttribute("coordinadorId", coordinadorId);
            model.addAttribute("establecimientos", establecimientoDeportivoRepository.findAll());
            return "admin/nuevaAsistencia";
        }

        // Validate overlapping assistances and maintenances
        LocalDateTime startCheck = horarioEntrada.minusMinutes(14).minusSeconds(59);
        LocalDateTime endCheck = horarioSalida.plusMinutes(14).plusSeconds(59);
        List<Asistencia> overlappingAsistencias = asistenciaRepository.findOverlappingAsistencias(coordinadorId, startCheck, endCheck);
        List<Mantenimiento> overlappingMantenimientos = mantenimientoRepository.findOverlappingMantenimientos(espacioDeportivoId, startCheck, endCheck);

        if (!overlappingAsistencias.isEmpty()) {
            attr.addFlashAttribute("msg", "El coordinador ya tiene una asistencia programada en ese horario");
            attr.addFlashAttribute("error", true);
            return "redirect:/admin/asistencias/nueva?coordinadorId=" + coordinadorId;
        }

        if (!overlappingMantenimientos.isEmpty()) {
            attr.addFlashAttribute("msg", "El espacio deportivo tiene un mantenimiento programado en ese horario");
            attr.addFlashAttribute("error", true);
            return "redirect:/admin/asistencias/nueva?coordinadorId=" + coordinadorId;
        }

        // Create assistance
        Asistencia asistencia = new Asistencia();
        asistencia.setCoordinador(optCoordinador.get());
        asistencia.setAdministrador(optAdministrador.get());
        asistencia.setEspacioDeportivo(optEspacio.get());
        asistencia.setHorarioEntrada(horarioEntrada);
        asistencia.setHorarioSalida(horarioSalida);
        asistencia.setEstadoEntrada(Asistencia.EstadoEntrada.pendiente);
        asistencia.setEstadoSalida(Asistencia.EstadoSalida.pendiente);
        asistencia.setFechaCreacion(LocalDateTime.now());

        asistenciaRepository.save(asistencia);

        // Create notification
        try {
            Optional<TipoNotificacion> optTipo = tipoNotificacionRepository.findAll().stream()
                    .filter(t -> t.getTipoNotificacion().equals("Nueva Asistencia Asignada"))
                    .findFirst();
            if (optTipo.isPresent()) {
                Notificacion notificacion = new Notificacion();
                notificacion.setUsuario(optCoordinador.get());
                notificacion.setTipoNotificacion(optTipo.get());
                notificacion.setTituloNotificacion("Nueva Asistencia Asignada");
                notificacion.setMensaje("Se te ha asignado una nueva asistencia para el " + fecha + " de " + horarioEntradaTime + " a " + horarioSalidaTime);
                notificacion.setUrlRedireccion("/coordinador/asistencia");
                notificacion.setFechaCreacion(LocalDateTime.now());
                notificacion.setEstado(Notificacion.Estado.no_leido);
                notificacionRepository.save(notificacion);
            }
        } catch (Exception e) {
            // Ignore notification failure as per requirement
        }

        attr.addFlashAttribute("msg", "Asistencia creada satisfactoriamente");
        return "redirect:/admin/coordinadores/calendario?id=" + coordinadorId;
    }

    // Helper class for horario response
    private static class HorarioResponse {
        private String horarioApertura;
        private String horarioCierre;

        public HorarioResponse(String horarioApertura, String horarioCierre) {
            this.horarioApertura = horarioApertura;
            this.horarioCierre = horarioCierre;
        }

        public String getHorarioApertura() {
            return horarioApertura;
        }

        public String getHorarioCierre() {
            return horarioCierre;
        }
    }

    // Existing methods (unchanged, included for completeness)
    @GetMapping("establecimientos")
    public String listarEstablecimientos(Model model) {
        List<EstablecimientoDeportivo> establecimientosList = establecimientoDeportivoRepository.findAll();
        model.addAttribute("establecimientos", establecimientosList);
        return "admin/establecimientoList";
    }

    @GetMapping("establecimientos/nuevo")
    public String crearEstablecimiento(@ModelAttribute("establecimiento") EstablecimientoDeportivo establecimiento, Model model) {
        return "admin/establecimientoForm";
    }


    @GetMapping("establecimientos/info")
    public String infoEstablecimiento(@RequestParam("id") Integer id, Model model, RedirectAttributes attr) {
        EstablecimientoDeportivo establecimiento = establecimientoDeportivoRepository.findByEstablecimientoDeportivoId(id);
        if (establecimiento == null) {
            attr.addFlashAttribute("message", "Establecimiento no encontrado.");
            attr.addFlashAttribute("messageType", "error");
            return "redirect:/admin/establecimientos";
        }
        List<EspacioDeportivo> espacios = espacioDeportivoRepository.findAllByEstablecimientoDeportivo(establecimiento);
        model.addAttribute("establecimiento", establecimiento);
        model.addAttribute("espacios", espacios);
        return "admin/establecimientoInfo";
    }


    // Show form to edit an existing establishment
    @GetMapping("establecimientos/editar")
    public String editarEstablecimiento(@ModelAttribute("establecimiento") EstablecimientoDeportivo establecimiento,
                                        @RequestParam("id") Integer id,
                                        Model model) {
        Optional<EstablecimientoDeportivo> optEstablecimiento = Optional.ofNullable(establecimientoDeportivoRepository.findByEstablecimientoDeportivoId(id));
        if (optEstablecimiento.isPresent()) {
            establecimiento = optEstablecimiento.get();
            model.addAttribute("establecimiento", establecimiento);
            // Pass parsed coordinates for map initialization
            if (establecimiento.getGeolocalizacion() != null && establecimiento.getGeolocalizacion().matches("^-?\\d+\\.\\d+,-?\\d+\\.\\d+$")) {
                String[] coords = establecimiento.getGeolocalizacion().split(",");
                model.addAttribute("latitude", Double.parseDouble(coords[0].trim()));
                model.addAttribute("longitude", Double.parseDouble(coords[1].trim()));
            } else {
                model.addAttribute("latitude", -12.043333); // Default
                model.addAttribute("longitude", -77.028333);
            }
            return "admin/establecimientoForm";
        } else {
            return "redirect:/admin/establecimientos";
        }
    }


    // Save establishment
    @PostMapping("establecimientos/guardar")
    public String guardarEstablecimiento(@ModelAttribute("establecimiento") @Valid EstablecimientoDeportivo establecimiento,
                                         BindingResult bindingResult,
                                         @RequestParam(value = "fotoFile", required = false) MultipartFile fotoFile,
                                         @RequestParam(value = "geolocalizacion", required = true) String geolocalizacion,
                                         Model model,
                                         RedirectAttributes attr) {
        // Validate geolocalizacion format
        if (geolocalizacion == null || !geolocalizacion.matches("^-?\\d+\\.\\d+,-?\\d+\\.\\d+$")) {
            bindingResult.rejectValue("geolocalizacion", "Pattern", "Formato inválido (ej: -12.04318,-77.02824)");
        } else {
            // Validate latitude and longitude ranges
            try {
                String[] coords = geolocalizacion.split(",");
                double latitude = Double.parseDouble(coords[0].trim());
                double longitude = Double.parseDouble(coords[1].trim());
                if (latitude < -90 || latitude > 90) {
                    bindingResult.rejectValue("geolocalizacion", "Range", "La latitud debe estar entre -90 y 90");
                }
                if (longitude < -180 || longitude > 180) {
                    bindingResult.rejectValue("geolocalizacion", "Range", "La longitud debe estar entre -180 y 180");
                }
            } catch (NumberFormatException e) {
                bindingResult.rejectValue("geolocalizacion", "Format", "Coordenadas inválidas");
            }
        }

        // Validate image format if provided
        String defaultFotoUrl = "https://media-cdn.tripadvisor.com/media/photo-s/12/34/6a/8f/cancha-de-futbol-redes.jpg";
        String existingFotoUrl = establecimiento.getEstablecimientoDeportivoId() != null ? establecimiento.getFotoEstablecimientoUrl() : null;
        boolean isCreation = establecimiento.getEstablecimientoDeportivoId() == null || establecimiento.getEstablecimientoDeportivoId() == 0;

        if (fotoFile != null && !fotoFile.isEmpty()) {
            String contentType = fotoFile.getContentType();
            if (contentType == null || !contentType.matches("^(image/(jpeg|png|jpg))$")) {
                bindingResult.rejectValue("fotoEstablecimientoUrl", "typeMismatch", "El archivo debe ser una imagen (JPEG, PNG o JPG)");
            } else {
                String uploadResult = s3Service.uploadFile(fotoFile);
                if (uploadResult != null && uploadResult.contains("URL:") && !uploadResult.trim().isEmpty()) {
                    String fotoUrl = uploadResult.substring(uploadResult.indexOf("URL: ") + 5).trim();
                    if (fotoUrl.length() > 255) {
                        bindingResult.rejectValue("fotoEstablecimientoUrl", "Size", "La URL de la foto no puede superar los 255 caracteres");
                    } else if (fotoUrl.isEmpty()) {
                        bindingResult.rejectValue("fotoEstablecimientoUrl", "Invalid", "La URL generada está vacía");
                    } else {
                        establecimiento.setFotoEstablecimientoUrl(fotoUrl);
                    }
                } else {
                    establecimiento.setFotoEstablecimientoUrl(isCreation || existingFotoUrl == null || existingFotoUrl.isEmpty() ? defaultFotoUrl : existingFotoUrl);
                    attr.addFlashAttribute("message", "Error al subir la foto: " + (uploadResult != null ? uploadResult : "Resultado inválido") + ". Se usó una imagen por defecto.");
                    attr.addFlashAttribute("messageType", "error");
                }
            }
        } else {
            establecimiento.setFotoEstablecimientoUrl(isCreation || existingFotoUrl == null || existingFotoUrl.isEmpty() ? defaultFotoUrl : existingFotoUrl);
        }

        if (bindingResult.hasErrors()) {
            return "admin/establecimientoForm";
        }

        // Set geolocalizacion
        establecimiento.setGeolocalizacion(geolocalizacion);

        // Ensure no empty string is saved
        if (establecimiento.getFotoEstablecimientoUrl() == null || establecimiento.getFotoEstablecimientoUrl().isEmpty()) {
            establecimiento.setFotoEstablecimientoUrl(defaultFotoUrl);
        }

        // Set creation/update timestamps
        if (isCreation) {
            establecimiento.setFechaCreacion(LocalDateTime.now());
            attr.addFlashAttribute("msg", "Establecimiento creado satisfactoriamente");
        } else {
            establecimiento.setFechaActualizacion(LocalDateTime.now());
            attr.addFlashAttribute("msg", "Establecimiento editado satisfactoriamente");
        }

        establecimientoDeportivoRepository.save(establecimiento);
        return "redirect:/admin/establecimientos";
    }



    // List all sports spaces
    @GetMapping("espacios")
    public String listarEspacios(Model model) {
        List<EspacioDeportivo> espaciosList = espacioDeportivoRepository.findAll();
        model.addAttribute("espacios", espaciosList);
        return "admin/espacioList";
    }

    // Show form to create a new sports space
    @GetMapping("espacios/nuevo")
    public String crearEspacioDeportivo(@ModelAttribute("espacio") EspacioDeportivo espacio, Model model) {
        model.addAttribute("establecimientos", establecimientoDeportivoRepository.findAll());
        model.addAttribute("servicios", servicioDeportivoRepository.findAll());
        return "admin/espacioForm";
    }


    // Show form to edit an existing sports space
    @GetMapping("espacios/editar")
    public String editarEspacioDeportivo(@ModelAttribute("espacio") EspacioDeportivo espacio,
                                         @RequestParam("id") Integer id,
                                         Model model) {
        Optional<EspacioDeportivo> optEspacio = Optional.ofNullable(espacioDeportivoRepository.findById(id).orElse(null));
        if (optEspacio.isPresent()) {
            espacio = optEspacio.get();
            model.addAttribute("espacio", espacio);
            model.addAttribute("establecimientos", establecimientoDeportivoRepository.findAll());
            model.addAttribute("servicios", servicioDeportivoRepository.findAll());
            // Pass parsed coordinates for map initialization
            if (espacio.getGeolocalizacion() != null && espacio.getGeolocalizacion().matches("^-?\\d+\\.\\d+,-?\\d+\\.\\d+$")) {
                String[] coords = espacio.getGeolocalizacion().split(",");
                model.addAttribute("latitude", Double.parseDouble(coords[0].trim()));
                model.addAttribute("longitude", Double.parseDouble(coords[1].trim()));
            } else {
                model.addAttribute("latitude", -12.043333); // Default
                model.addAttribute("longitude", -77.028333);
            }
            return "admin/espacioForm";
        } else {
            return "redirect:/admin/establecimientos";
        }
    }


    // Save sports space
    @PostMapping("espacios/guardar")
    public String guardarEspacioDeportivo(@ModelAttribute("espacio") @Valid EspacioDeportivo espacio,
                                          BindingResult bindingResult,
                                          @RequestParam(value = "fotoFile", required = false) MultipartFile fotoFile,
                                          @RequestParam(value = "geolocalizacion", required = true) String geolocalizacion,
                                          Model model,
                                          RedirectAttributes attr) {
        // Validate geolocalizacion format
        if (geolocalizacion == null || !geolocalizacion.matches("^-?\\d+\\.\\d+,-?\\d+\\.\\d+$")) {
            bindingResult.rejectValue("geolocalizacion", "Pattern", "Formato inválido (ej: -12.04318,-77.02824)");
        } else {
            // Validate latitude and longitude ranges
            try {
                String[] coords = geolocalizacion.split(",");
                double latitude = Double.parseDouble(coords[0].trim());
                double longitude = Double.parseDouble(coords[1].trim());
                if (latitude < -90 || latitude > 90) {
                    bindingResult.rejectValue("geolocalizacion", "Range", "La latitud debe estar entre -90 y 90");
                }
                if (longitude < -180 || longitude > 180) {
                    bindingResult.rejectValue("geolocalizacion", "Range", "La longitud debe estar entre -180 y 180");
                }
            } catch (NumberFormatException e) {
                bindingResult.rejectValue("geolocalizacion", "Format", "Coordenadas inválidas");
            }
        }

        // Validate image format if provided
        String defaultFotoUrl = "https://media-cdn.tripadvisor.com/media/photo-s/12/34/6a/8f/cancha-de-futbol-redes.jpg";
        String existingFotoUrl = espacio.getEspacioDeportivoId() != null ? espacio.getFotoEspacioDeportivoUrl() : null;
        boolean isCreation = espacio.getEspacioDeportivoId() == null || espacio.getEspacioDeportivoId() == 0;

        if (fotoFile != null && !fotoFile.isEmpty()) {
            String contentType = fotoFile.getContentType();
            if (contentType == null || !contentType.matches("^(image/(jpeg|png|jpg))$")) {
                bindingResult.rejectValue("fotoEspacioDeportivoUrl", "typeMismatch", "El archivo debe ser una imagen (JPEG, PNG o JPG)");
            } else {
                String uploadResult = s3Service.uploadFile(fotoFile);
                if (uploadResult != null && uploadResult.contains("URL:") && !uploadResult.trim().isEmpty()) {
                    String fotoUrl = uploadResult.substring(uploadResult.indexOf("URL: ") + 5).trim();
                    if (fotoUrl.length() > 255) {
                        bindingResult.rejectValue("fotoEspacioDeportivoUrl", "Size", "La URL de la foto no puede superar los 255 caracteres");
                    } else if (fotoUrl.isEmpty()) {
                        bindingResult.rejectValue("fotoEspacioDeportivoUrl", "Invalid", "La URL generada está vacía");
                    } else {
                        espacio.setFotoEspacioDeportivoUrl(fotoUrl);
                    }
                } else {
                    espacio.setFotoEspacioDeportivoUrl(isCreation || existingFotoUrl == null || existingFotoUrl.isEmpty() ? defaultFotoUrl : existingFotoUrl);
                    attr.addFlashAttribute("message", "Error al subir la foto: " + (uploadResult != null ? uploadResult : "Resultado inválido") + ". Se usó una imagen por defecto.");
                    attr.addFlashAttribute("messageType", "error");
                }
            }
        } else {
            espacio.setFotoEspacioDeportivoUrl(isCreation || existingFotoUrl == null || existingFotoUrl.isEmpty() ? defaultFotoUrl : existingFotoUrl);
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("establecimientos", establecimientoDeportivoRepository.findAll());
            model.addAttribute("servicios", servicioDeportivoRepository.findAll());
            return "admin/espacioForm";
        }

        // Set geolocalizacion
        espacio.setGeolocalizacion(geolocalizacion);

        // Ensure no empty string is saved
        if (espacio.getFotoEspacioDeportivoUrl() == null || espacio.getFotoEspacioDeportivoUrl().isEmpty()) {
            espacio.setFotoEspacioDeportivoUrl(defaultFotoUrl);
        }

        // Set creation/update timestamps
        if (isCreation) {
            espacio.setFechaCreacion(LocalDateTime.now());
            attr.addFlashAttribute("msg", "Espacio creado satisfactoriamente");
        } else {
            attr.addFlashAttribute("msg", "Espacio editado satisfactoriamente");
        }
        espacio.setFechaActualizacion(LocalDateTime.now());

        espacioDeportivoRepository.save(espacio);
        return "redirect:/admin/establecimientos/info?id=" + espacio.getEstablecimientoDeportivo().getEstablecimientoDeportivoId();
    }




    @GetMapping("espacios/detalle")
    public String detalleEspacioDeportivo(@RequestParam Integer id, Model model, RedirectAttributes attr) {
        Optional<EspacioDeportivo> optEspacio = espacioDeportivoRepository.findById(id);
        if (optEspacio.isEmpty()) {
            attr.addFlashAttribute("message", "Espacio no encontrado.");
            attr.addFlashAttribute("messageType", "error");
            return "redirect:/admin/establecimientos";
        }
        EspacioDeportivo espacio = optEspacio.get();
        model.addAttribute("espacio", espacio);
        return "admin/espacioInfo";
    }

    @GetMapping("coordinadores")
    public String listarCoordinadores(Model model) {
        List<Usuario> usuariosList = usuarioRepository.findAllByRol_Rol("coordinador");
        model.addAttribute("coordinadores", usuariosList);
        return "admin/coordinadorList";
    }

    @GetMapping("coordinadores/calendario")
    public String calendarioCoordinadores(@RequestParam Integer id, Model model) {
        Usuario coordinador = usuarioRepository.findByUsuarioId(id);
        model.addAttribute("coordinador", coordinador);
        return "admin/coordinadorCalendario";
    }

    @GetMapping("/perfil")
    public String perfilAdministrador(@SessionAttribute("usuario") Usuario usuario, Model model) {
        Optional<Usuario> usuarioOptional = usuarioRepository.findById(usuario.getUsuarioId());
        if (usuarioOptional.isPresent()) {
            return "admin/adminPerfil";
        }
        return "redirect:/admin/dashboard";
    }


    @GetMapping("/perfil/editar")
    public String editarPerfilAdministrador(@SessionAttribute("usuario") Usuario usuario, Model model) {
        Optional<Usuario> usuarioOptional = usuarioRepository.findById(usuario.getUsuarioId());
        if (usuarioOptional.isPresent()) {
            model.addAttribute("usuario", usuarioOptional.get());
            return "admin/editarPerfil";
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/perfil/actualizar")
    public String actualizarPerfil(
            @Valid @ModelAttribute("usuario") Usuario usuarioActualizado,
            BindingResult result,
            @RequestParam(value = "fotoPerfil", required = false) MultipartFile fotoPerfil,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        boolean isCreation = usuario.getUsuarioId() == null;

        // Validate image format if provided
        String defaultFotoPerfilUrl = "https://img.freepik.com/foto-gratis/disparo-cabeza-hombre-atractivo-sonriendo-complacido-mirando-intrigado-pie-sobre-fondo-azul_1258-65733.jpg";
        String existingFotoPerfilUrl = usuario.getFotoPerfilUrl();
        if (fotoPerfil != null && !fotoPerfil.isEmpty()) {
            String contentType = fotoPerfil.getContentType();
            if (contentType == null || !contentType.matches("^(image/(jpeg|png|jpg))$")) {
                result.rejectValue("fotoPerfilUrl", "typeMismatch", "El archivo debe ser una imagen (JPEG, PNG o JPG)");
            } else {
                String uploadResult = s3Service.uploadFile(fotoPerfil);
                if (uploadResult != null && uploadResult.contains("URL:") && !uploadResult.trim().isEmpty()) {
                    String fotoUrl = uploadResult.substring(uploadResult.indexOf("URL: ") + 5).trim();
                    if (fotoUrl.length() > 255) {
                        result.rejectValue("fotoPerfilUrl", "Size", "La URL de la foto no puede superar los 255 caracteres");
                    } else if (fotoUrl.isEmpty()) {
                        result.rejectValue("fotoPerfilUrl", "Invalid", "La URL generada está vacía");
                    } else {
                        usuario.setFotoPerfilUrl(fotoUrl);
                    }
                } else {
                    usuario.setFotoPerfilUrl(isCreation || existingFotoPerfilUrl == null || existingFotoPerfilUrl.isEmpty() ? defaultFotoPerfilUrl : existingFotoPerfilUrl);
                    redirectAttributes.addFlashAttribute("message", "Error al subir la foto: " +
                            (uploadResult != null ? uploadResult : "Resultado inválido") + ". Se usó una imagen por defecto.");
                    redirectAttributes.addFlashAttribute("messageType", "error");
                }
            }
        } else {
            usuario.setFotoPerfilUrl(isCreation || existingFotoPerfilUrl == null || existingFotoPerfilUrl.isEmpty() ? defaultFotoPerfilUrl : existingFotoPerfilUrl);
        }

        // If there are validation errors, return the form
        if (result.hasErrors()) {
            return "admin/editarPerfil";
        }

        // Update all editable fields
        usuario.setNombres(usuarioActualizado.getNombres());
        usuario.setApellidos(usuarioActualizado.getApellidos());
        usuario.setCorreoElectronico(usuarioActualizado.getCorreoElectronico());
        usuario.setDni(usuarioActualizado.getDni());
        usuario.setDireccion(usuarioActualizado.getDireccion());
        usuario.setTelefono(usuarioActualizado.getTelefono());

        // Ensure fotoPerfilUrl is never empty
        if (usuario.getFotoPerfilUrl() == null || usuario.getFotoPerfilUrl().isEmpty()) {
            usuario.setFotoPerfilUrl(defaultFotoPerfilUrl);
        }

        // Update timestamp
        usuario.setFechaActualizacion(LocalDateTime.now());

        // Save changes
        usuarioRepository.save(usuario);

        // Update session
        session.setAttribute("usuario", usuario);

        redirectAttributes.addFlashAttribute("msg", "Perfil actualizado satisfactoriamente");
        return "redirect:/admin/perfil";
    }


    @GetMapping("pagos")
    public String listarPagos(Model model) {
        List<Pago> pagosPendientes = pagoRepository.findByEstadoTransaccionAndMetodoPago_MetodoPagoId(
                Pago.EstadoTransaccion.pendiente, 1);
        model.addAttribute("pagosPendientes", pagosPendientes);
        return "admin/pagosList";
    }

    @GetMapping("/pagos/aceptar")
    public String aceptarPago(@RequestParam("id") Integer id) {
        Optional<Pago> optPago = pagoRepository.findById(id);
        if (optPago.isPresent()) {
            Pago pago = optPago.get();
            pago.setEstadoTransaccion(Pago.EstadoTransaccion.completado);
            pagoRepository.save(pago);
        }
        return "redirect:/admin/pagos";
    }

    @GetMapping("/pagos/rechazar")
    public String rechazarPago(@RequestParam("id") Integer id) {
        Optional<Pago> optPago = pagoRepository.findById(id);
        if (optPago.isPresent()) {
            Pago pago = optPago.get();
            pago.setEstadoTransaccion(Pago.EstadoTransaccion.fallido);
            pagoRepository.save(pago);
        }
        return "redirect:/admin/pagos";
    }

    @GetMapping("/pagos/ver")
    public String verDetallePago(@RequestParam("id") Integer id, Model model) {
        Optional<Pago> optPago = pagoRepository.findById(id);
        if (optPago.isPresent()) {
            model.addAttribute("pago", optPago.get());
            return "admin/pagosInfo";
        }
        return "redirect:/admin/pagos/pendientes/transaccion";
    }

    @GetMapping("/observaciones")
    public String listarObservaciones(
            @RequestParam(required = false) String nivel,
            Model model) {
        List<Observacion.Estado> estadosVisibles = List.of(Observacion.Estado.pendiente, Observacion.Estado.en_proceso);
        List<Observacion> observaciones;
        if (nivel == null || nivel.equals("sin_filtro")) {
            observaciones = observacionRepository.findByEstadoInOrderByEstadoAsc(estadosVisibles);
        } else {
            try {
                Observacion.NivelUrgencia urgencia = Observacion.NivelUrgencia.valueOf(nivel);
                observaciones = observacionRepository.findByEstadoInAndNivelUrgenciaOrderByEstadoAsc(estadosVisibles, urgencia);
            } catch (IllegalArgumentException e) {
                observaciones = observacionRepository.findByEstadoInOrderByEstadoAsc(estadosVisibles);
            }
        }
        model.addAttribute("observaciones", observaciones);
        model.addAttribute("nivelSeleccionado", nivel == null ? "sin_filtro" : nivel);
        return "admin/observacionesList";
    }

    @GetMapping("/observaciones/info")
    public String verInfoObservacion(@RequestParam("id") Integer id, Model model) {
        Observacion observacion = observacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ID inválido"));
        if (observacion.getEstado() == Observacion.Estado.pendiente) {
            observacion.setEstado(Observacion.Estado.en_proceso);
            observacion.setFechaActualizacion(LocalDateTime.now());
            observacionRepository.save(observacion);
        }
        model.addAttribute("observacion", observacion);
        return "admin/observacionInfo";
    }

    @PostMapping("/observaciones/resolver")
    public String resolverObservacion(@RequestParam("id") Integer id,
                                      @RequestParam("comentarioAdministrador") String comentario) {
        Observacion observacion = observacionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("ID inválido"));
        observacion.setEstado(Observacion.Estado.resuelto);
        observacion.setComentarioAdministrador(comentario);
        observacion.setFechaActualizacion(LocalDateTime.now());
        observacionRepository.save(observacion);
        return "redirect:/admin/observaciones";
    }

    @GetMapping("/dashboard")
    public String estadisticas(Model model) {
        Integer numeroReservasMes = reservaRepository.numeroReservasMes();
        Integer numeroReservasMesPasado = reservaRepository.numeroReservasMesPasado();
        Double montoMensual = reservaRepository.obtenerMontoTotalDeReservasEsteMes();
        montoMensual = montoMensual == null ? 0 : montoMensual;
        Double promedioMensual = montoMensual / numeroReservasMes;
        promedioMensual = montoMensual == 0.0 ? 0.0 : promedioMensual;
        Double montoMensualPasado = reservaRepository.obtenerMontoTotalDeReservasMesPasado();
        montoMensualPasado = (montoMensualPasado == null) ? 0.0 : montoMensualPasado;
        double promedioMensualPasado = montoMensualPasado / numeroReservasMesPasado;
        promedioMensualPasado = (montoMensualPasado == 0.0) ? 0.0 : promedioMensualPasado;

        String badge;
        long diferencia = numeroReservasMes - numeroReservasMesPasado;
        if (diferencia < 0) {
            badge = "badge bg-danger-subtle text-danger font-size-11";
        } else {
            badge = "badge bg-success-subtle text-success font-size-11";
        }

        model.addAttribute("numeroReservasMes", numeroReservasMes);
        model.addAttribute("promedioMensualPasado", promedioMensualPasado);
        model.addAttribute("diferencia", diferencia);
        model.addAttribute("badge", badge);
        model.addAttribute("montoMensual", montoMensual);
        model.addAttribute("promedioMensual", promedioMensual);
        model.addAttribute("montoMensualPasado", montoMensualPasado);

        List<CantidadReservasPorDiaDto> reservasPorDia = usuarioRepository.obtenerCantidadReservasPorDia();
        int totalReservas = reservasPorDia.stream()
                .mapToInt(CantidadReservasPorDiaDto::getCantidadReservas)
                .sum();

        List<Integer> chartData = new ArrayList<>();
        List<String> chartLabels = new ArrayList<>();
        List<Object[]> topDias = new ArrayList<>();

        for (CantidadReservasPorDiaDto reserva : reservasPorDia) {
            chartData.add(reserva.getCantidadReservas());
            chartLabels.add(reserva.getDia());
            double porcentaje = (reserva.getCantidadReservas() * 100.0) / totalReservas;
            topDias.add(new Object[]{reserva.getDia(), String.format("%.1f%%", porcentaje)});
        }

        topDias.sort((a, b) -> Double.compare(
                Double.parseDouble(((String) b[1]).replace("%", "")),
                Double.parseDouble(((String) a[1]).replace("%", ""))
        ));
        List<Object[]> top3Dias = topDias.size() > 3 ? topDias.subList(0, 3) : topDias;

        model.addAttribute("reservasPorDia", reservasPorDia);
        model.addAttribute("chartData", chartData);
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("top3Dias", top3Dias);

        Aviso ultimoAviso = avisoRepository.findLatestAviso();
        model.addAttribute("ultimoAviso", ultimoAviso);

        return "admin/dashboard";
    }

    @GetMapping("/espacios/calendario")
    public String mostrarCalendarioEspacio(@RequestParam("id") Integer espacioId, Model model) {
        EspacioDeportivo espacio = espacioDeportivoRepository.findById(espacioId)
                .orElseThrow(() -> new IllegalArgumentException("Espacio deportivo no encontrado"));
        model.addAttribute("espacio", espacio);
        return "admin/espacioCalendario";
    }

    @GetMapping("/calendario/reservas/{espacioId}")
    @ResponseBody
    public List<EventoCalendarioDTO> obtenerReservas(
            @PathVariable Integer espacioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return calendarioService.obtenerReservasConsolidadas(espacioId, start, end);
    }

    @GetMapping("/calendario/asistencias/{espacioId}")
    @ResponseBody
    public List<EventoCalendarioDTO> obtenerAsistencias(
            @PathVariable Integer espacioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return calendarioService.obtenerAsistencias(espacioId, start, end);
    }
}