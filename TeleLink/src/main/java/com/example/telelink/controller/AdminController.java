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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Apache POI imports for Excel
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

// iText imports for PDF
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPCell;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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

    @Autowired
    private ReembolsoRepository reembolsoRepository;

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
                Pago.EstadoTransaccion.pendiente, 2);
        model.addAttribute("pagosPendientes", pagosPendientes);
        return "admin/pagosList";
    }

    /*@GetMapping("/pagos/aceptar")
    public String aceptarPago(@RequestParam("id") Integer id) {
        Optional<Pago> optPago = pagoRepository.findById(id);
        if (optPago.isPresent()) {
            Pago pago = optPago.get();
            pago.setEstadoTransaccion(Pago.EstadoTransaccion.completado);
            pago.setDetallesTransaccion("Pago aceptado por el administrador");
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

     */

    @GetMapping("/pagos/aceptar")
    public String aceptarPago(@RequestParam("id") Integer id) {
        Optional<Pago> optPago = pagoRepository.findById(id);
        if (optPago.isPresent()) {
            Pago pago = optPago.get();
            pago.setEstadoTransaccion(Pago.EstadoTransaccion.completado);
            pago.setDetallesTransaccion("Pago aceptado por el administrador");

            // Update the associated Reserva to 'completada'
            Reserva reserva = pago.getReserva();
            if (reserva != null) {
                reserva.setEstado(Reserva.Estado.completada);
                reserva.setFechaActualizacion(LocalDateTime.now()); // Update timestamp
                reservaRepository.save(reserva);
            }

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

            // Ensure the associated Reserva remains 'pendiente' or update if necessary
            Reserva reserva = pago.getReserva();
            if (reserva != null && !reserva.getEstado().equals(Reserva.Estado.pendiente)) {
                reserva.setEstado(Reserva.Estado.pendiente);
                reserva.setFechaActualizacion(LocalDateTime.now()); // Update timestamp
                reservaRepository.save(reserva);
            }

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

    // ================= EXPORT METHODS FOR ADMIN TABLES =================

    // Export methods for Observaciones
    @GetMapping("/observaciones/export/excel")
    public ResponseEntity<byte[]> exportarObservacionesExcel() {
        try {
            List<Observacion> observaciones = observacionRepository.findAll();

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Observaciones");

            // Estilo para el encabezado
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);

            // Estilo para las celdas de datos
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setAlignment(HorizontalAlignment.LEFT);

            // Estilo para fechas
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.cloneStyleFrom(dataStyle);
            dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy"));

            // Crear encabezados
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Fecha", "Espacio Deportivo", "Establecimiento", "Nivel de Urgencia", "Coordinador", "Estado"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Llenar datos
            int rowNum = 1;
            for (Observacion observacion : observaciones) {
                Row row = sheet.createRow(rowNum++);

                Cell dateCell = row.createCell(0);
                dateCell.setCellValue(observacion.getFechaCreacion());
                dateCell.setCellStyle(dateStyle);

                Cell espacioCell = row.createCell(1);
                espacioCell.setCellValue(observacion.getEspacioDeportivo().getNombre());
                espacioCell.setCellStyle(dataStyle);

                Cell establecimientoCell = row.createCell(2);
                establecimientoCell.setCellValue(observacion.getEspacioDeportivo().getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre());
                establecimientoCell.setCellStyle(dataStyle);

                Cell urgenciaCell = row.createCell(3);
                urgenciaCell.setCellValue(observacion.getNivelUrgencia().name().substring(0, 1).toUpperCase() +
                                         observacion.getNivelUrgencia().name().substring(1).toLowerCase());
                urgenciaCell.setCellStyle(dataStyle);

                Cell coordinadorCell = row.createCell(4);
                coordinadorCell.setCellValue(observacion.getCoordinador().getNombres() + " " + observacion.getCoordinador().getApellidos());
                coordinadorCell.setCellStyle(dataStyle);

                Cell estadoCell = row.createCell(5);
                estadoCell.setCellValue(getEstadoObservacionTexto(observacion.getEstado()));
                estadoCell.setCellStyle(dataStyle);
            }

            // Ajustar ancho de columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) < 3000) {
                    sheet.setColumnWidth(i, 3000);
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            responseHeaders.setContentDispositionFormData("attachment", "observaciones_admin.xlsx");

            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(outputStream.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/observaciones/export/pdf")
    public ResponseEntity<byte[]> exportarObservacionesPdf() {
        try {
            List<Observacion> observaciones = observacionRepository.findAll();

            Document document = new Document(PageSize.A4.rotate());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, outputStream);

            document.open();

            // Título
            com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
            Paragraph title = new Paragraph("Reporte de Observaciones - Admin", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Fecha de generación
            com.itextpdf.text.Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
            Paragraph dateGenerated = new Paragraph("Fecha de generación: " +
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), dateFont);
            dateGenerated.setAlignment(Element.ALIGN_CENTER);
            dateGenerated.setSpacingAfter(20);
            document.add(dateGenerated);

            // Crear tabla
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // Configurar anchos de columnas
            float[] columnWidths = {2f, 3f, 3f, 2f, 3f, 2f};
            table.setWidths(columnWidths);

            // Estilo para encabezados
            com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
            BaseColor headerColor = new BaseColor(52, 58, 64);

            // Agregar encabezados
            String[] headers = {"Fecha", "Espacio Deportivo", "Establecimiento", "Nivel Urgencia", "Coordinador", "Estado"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(headerColor);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(8);
                table.addCell(cell);
            }

            // Estilo para datos
            com.itextpdf.text.Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

            // Agregar datos
            for (Observacion observacion : observaciones) {
                PdfPCell dateCell = new PdfPCell(new Phrase(
                    observacion.getFechaCreacion().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")), dataFont));
                dateCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                dateCell.setPadding(5);
                table.addCell(dateCell);

                PdfPCell espacioCell = new PdfPCell(new Phrase(observacion.getEspacioDeportivo().getNombre(), dataFont));
                espacioCell.setPadding(5);
                table.addCell(espacioCell);

                PdfPCell establecimientoCell = new PdfPCell(new Phrase(
                    observacion.getEspacioDeportivo().getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre(), dataFont));
                establecimientoCell.setPadding(5);
                table.addCell(establecimientoCell);

                PdfPCell urgenciaCell = new PdfPCell(new Phrase(
                    observacion.getNivelUrgencia().name().substring(0, 1).toUpperCase() +
                    observacion.getNivelUrgencia().name().substring(1).toLowerCase(), dataFont));
                urgenciaCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                urgenciaCell.setPadding(5);
                table.addCell(urgenciaCell);

                PdfPCell coordinadorCell = new PdfPCell(new Phrase(
                    observacion.getCoordinador().getNombres() + " " + observacion.getCoordinador().getApellidos(), dataFont));
                coordinadorCell.setPadding(5);
                table.addCell(coordinadorCell);

                PdfPCell estadoCell = new PdfPCell(new Phrase(getEstadoObservacionTexto(observacion.getEstado()), dataFont));
                estadoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                estadoCell.setPadding(5);
                table.addCell(estadoCell);
            }

            document.add(table);
            document.close();

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_PDF);
            responseHeaders.setContentDispositionFormData("attachment", "observaciones_admin.pdf");

            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(outputStream.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Export methods for Pagos
    @GetMapping("/pagos/export/excel")
    public ResponseEntity<byte[]> exportarPagosExcel() {
        try {
            List<Pago> pagos = pagoRepository.findAll();

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Pagos");

            // Estilo para el encabezado
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);

            // Estilo para las celdas de datos
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setAlignment(HorizontalAlignment.LEFT);

            // Estilo para fechas
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.cloneStyleFrom(dataStyle);
            dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy hh:mm"));

            // Crear encabezados
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Usuario", "Establecimiento Deportivo", "Monto", "Estado", "Fecha y Hora"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Llenar datos
            int rowNum = 1;
            for (Pago pago : pagos) {
                Row row = sheet.createRow(rowNum++);

                Cell usuarioCell = row.createCell(0);
                usuarioCell.setCellValue(pago.getReserva().getUsuario().getNombres() + " " + pago.getReserva().getUsuario().getApellidos());
                usuarioCell.setCellStyle(dataStyle);

                Cell establecimientoCell = row.createCell(1);
                establecimientoCell.setCellValue(pago.getReserva().getEspacioDeportivo().getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre());
                establecimientoCell.setCellStyle(dataStyle);

                Cell montoCell = row.createCell(2);
                montoCell.setCellValue("S/ " + pago.getMonto().toString());
                montoCell.setCellStyle(dataStyle);

                Cell estadoCell = row.createCell(3);
                estadoCell.setCellValue(getEstadoPagoTexto(pago.getEstadoTransaccion()));
                estadoCell.setCellStyle(dataStyle);

                Cell fechaCell = row.createCell(4);
                fechaCell.setCellValue(pago.getFechaPago());
                fechaCell.setCellStyle(dateStyle);
            }

            // Ajustar ancho de columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) < 3000) {
                    sheet.setColumnWidth(i, 3000);
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            responseHeaders.setContentDispositionFormData("attachment", "pagos_admin.xlsx");

            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(outputStream.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/pagos/export/pdf")
    public ResponseEntity<byte[]> exportarPagosPdf() {
        try {
            List<Pago> pagos = pagoRepository.findAll();

            Document document = new Document(PageSize.A4.rotate());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, outputStream);

            document.open();

            // Título
            com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
            Paragraph title = new Paragraph("Reporte de Pagos - Admin", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Fecha de generación
            com.itextpdf.text.Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
            Paragraph dateGenerated = new Paragraph("Fecha de generación: " +
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), dateFont);
            dateGenerated.setAlignment(Element.ALIGN_CENTER);
            dateGenerated.setSpacingAfter(20);
            document.add(dateGenerated);

            // Crear tabla
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // Configurar anchos de columnas
            float[] columnWidths = {3f, 4f, 2f, 2f, 3f};
            table.setWidths(columnWidths);

            // Estilo para encabezados
            com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
            BaseColor headerColor = new BaseColor(52, 58, 64);

            // Agregar encabezados
            String[] headers = {"Usuario", "Establecimiento Deportivo", "Monto", "Estado", "Fecha y Hora"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(headerColor);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(8);
                table.addCell(cell);
            }

            // Estilo para datos
            com.itextpdf.text.Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

            // Agregar datos
            for (Pago pago : pagos) {
                PdfPCell usuarioCell = new PdfPCell(new Phrase(
                    pago.getReserva().getUsuario().getNombres() + " " + pago.getReserva().getUsuario().getApellidos(), dataFont));
                usuarioCell.setPadding(5);
                table.addCell(usuarioCell);

                PdfPCell establecimientoCell = new PdfPCell(new Phrase(
                    pago.getReserva().getEspacioDeportivo().getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre(), dataFont));
                establecimientoCell.setPadding(5);
                table.addCell(establecimientoCell);

                PdfPCell montoCell = new PdfPCell(new Phrase("S/ " + pago.getMonto().toString(), dataFont));
                montoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                montoCell.setPadding(5);
                table.addCell(montoCell);

                PdfPCell estadoCell = new PdfPCell(new Phrase(getEstadoPagoTexto(pago.getEstadoTransaccion()), dataFont));
                estadoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                estadoCell.setPadding(5);
                table.addCell(estadoCell);

                PdfPCell fechaCell = new PdfPCell(new Phrase(
                    pago.getFechaPago().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), dataFont));
                fechaCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                fechaCell.setPadding(5);
                table.addCell(fechaCell);
            }

            document.add(table);
            document.close();

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_PDF);
            responseHeaders.setContentDispositionFormData("attachment", "pagos_admin.pdf");

            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(outputStream.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }    // Export methods for Coordinadores
    @GetMapping("/coordinadores/export/excel")
    public ResponseEntity<byte[]> exportarCoordinadoresExcel() {
        try {
            List<Usuario> coordinadores = usuarioRepository.findAllByRol_Rol("coordinador");

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Coordinadores");

            // Estilo para el encabezado
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);

            // Estilo para las celdas de datos
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setAlignment(HorizontalAlignment.LEFT);

            // Crear encabezados
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Nombre Completo", "Correo Electrónico", "Teléfono"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Llenar datos
            int rowNum = 1;
            for (Usuario coordinador : coordinadores) {
                Row row = sheet.createRow(rowNum++);

                Cell nombreCell = row.createCell(0);
                nombreCell.setCellValue(coordinador.getNombres() + " " + coordinador.getApellidos());
                nombreCell.setCellStyle(dataStyle);

                Cell correoCell = row.createCell(1);
                correoCell.setCellValue(coordinador.getCorreoElectronico());
                correoCell.setCellStyle(dataStyle);

                Cell telefonoCell = row.createCell(2);
                telefonoCell.setCellValue(coordinador.getTelefono() != null ? coordinador.getTelefono() : "");
                telefonoCell.setCellStyle(dataStyle);
            }

            // Ajustar ancho de columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) < 3000) {
                    sheet.setColumnWidth(i, 3000);
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            responseHeaders.setContentDispositionFormData("attachment", "coordinadores_admin.xlsx");

            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(outputStream.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();        }
    }

    @GetMapping("/coordinadores/export/pdf")
    public ResponseEntity<byte[]> exportarCoordinadoresPdf() {
        try {
            List<Usuario> coordinadores = usuarioRepository.findAllByRol_Rol("coordinador");

            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, outputStream);

            document.open();

            // Título
            com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
            Paragraph title = new Paragraph("Reporte de Coordinadores - Admin", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Fecha de generación
            com.itextpdf.text.Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
            Paragraph dateGenerated = new Paragraph("Fecha de generación: " +
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), dateFont);
            dateGenerated.setAlignment(Element.ALIGN_CENTER);
            dateGenerated.setSpacingAfter(20);
            document.add(dateGenerated);

            // Crear tabla
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // Configurar anchos de columnas
            float[] columnWidths = {4f, 4f, 2f};
            table.setWidths(columnWidths);

            // Estilo para encabezados
            com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
            BaseColor headerColor = new BaseColor(52, 58, 64);

            // Agregar encabezados
            String[] headers = {"Nombre Completo", "Correo Electrónico", "Teléfono"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(headerColor);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(8);
                table.addCell(cell);
            }

            // Estilo para datos
            com.itextpdf.text.Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            // Agregar datos
            for (Usuario coordinador : coordinadores) {
                PdfPCell nombreCell = new PdfPCell(new Phrase(
                    coordinador.getNombres() + " " + coordinador.getApellidos(), dataFont));
                nombreCell.setPadding(5);
                table.addCell(nombreCell);

                PdfPCell correoCell = new PdfPCell(new Phrase(coordinador.getCorreoElectronico(), dataFont));
                correoCell.setPadding(5);
                table.addCell(correoCell);

                PdfPCell telefonoCell = new PdfPCell(new Phrase(
                    coordinador.getTelefono() != null ? coordinador.getTelefono() : "", dataFont));
                telefonoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                telefonoCell.setPadding(5);
                table.addCell(telefonoCell);
            }

            document.add(table);
            document.close();

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_PDF);
            responseHeaders.setContentDispositionFormData("attachment", "coordinadores_admin.pdf");

            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(outputStream.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Export methods for Establecimientos
    @GetMapping("/establecimientos/export/excel")
    public ResponseEntity<byte[]> exportarEstablecimientosExcel() {
        try {
            List<EstablecimientoDeportivo> establecimientos = establecimientoDeportivoRepository.findAll();

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Establecimientos");

            // Estilo para el encabezado
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);

            // Estilo para las celdas de datos
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setAlignment(HorizontalAlignment.LEFT);

            // Crear encabezados
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Nombre", "Dirección", "Teléfono", "Correo", "Horario"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Llenar datos
            int rowNum = 1;
            for (EstablecimientoDeportivo establecimiento : establecimientos) {
                Row row = sheet.createRow(rowNum++);

                Cell nombreCell = row.createCell(0);
                nombreCell.setCellValue(establecimiento.getEstablecimientoDeportivoNombre());
                nombreCell.setCellStyle(dataStyle);

                Cell direccionCell = row.createCell(1);
                direccionCell.setCellValue(establecimiento.getDireccion());
                direccionCell.setCellStyle(dataStyle);

                Cell telefonoCell = row.createCell(2);
                telefonoCell.setCellValue(establecimiento.getTelefonoContacto() != null ? establecimiento.getTelefonoContacto() : "");
                telefonoCell.setCellStyle(dataStyle);

                Cell correoCell = row.createCell(3);
                correoCell.setCellValue(establecimiento.getCorreoContacto() != null ? establecimiento.getCorreoContacto() : "");
                correoCell.setCellStyle(dataStyle);

                Cell horarioCell = row.createCell(4);
                String horario = (establecimiento.getHorarioApertura() != null ? establecimiento.getHorarioApertura().toString() : "") +
                                " - " +
                                (establecimiento.getHorarioCierre() != null ? establecimiento.getHorarioCierre().toString() : "");
                horarioCell.setCellValue(horario);
                horarioCell.setCellStyle(dataStyle);
            }

            // Ajustar ancho de columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) < 3000) {
                    sheet.setColumnWidth(i, 3000);
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            responseHeaders.setContentDispositionFormData("attachment", "establecimientos_admin.xlsx");

            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(outputStream.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/establecimientos/export/pdf")
    public ResponseEntity<byte[]> exportarEstablecimientosPdf() {
        try {
            List<EstablecimientoDeportivo> establecimientos = establecimientoDeportivoRepository.findAll();

            Document document = new Document(PageSize.A4.rotate());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, outputStream);

            document.open();

            // Título
            com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
            Paragraph title = new Paragraph("Reporte de Establecimientos Deportivos - Admin", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Fecha de generación
            com.itextpdf.text.Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
            Paragraph dateGenerated = new Paragraph("Fecha de generación: " +
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), dateFont);
            dateGenerated.setAlignment(Element.ALIGN_CENTER);
            dateGenerated.setSpacingAfter(20);
            document.add(dateGenerated);

            // Crear tabla
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // Configurar anchos de columnas
            float[] columnWidths = {3f, 4f, 2f, 3f, 3f};
            table.setWidths(columnWidths);

            // Estilo para encabezados
            com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
            BaseColor headerColor = new BaseColor(52, 58, 64);

            // Agregar encabezados
            String[] headers = {"Nombre", "Dirección", "Teléfono", "Correo", "Horario"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(headerColor);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(8);
                table.addCell(cell);
            }

            // Estilo para datos
            com.itextpdf.text.Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

            // Agregar datos
            for (EstablecimientoDeportivo establecimiento : establecimientos) {
                PdfPCell nombreCell = new PdfPCell(new Phrase(establecimiento.getEstablecimientoDeportivoNombre(), dataFont));
                nombreCell.setPadding(5);
                table.addCell(nombreCell);

                PdfPCell direccionCell = new PdfPCell(new Phrase(establecimiento.getDireccion(), dataFont));
                direccionCell.setPadding(5);
                table.addCell(direccionCell);

                PdfPCell telefonoCell = new PdfPCell(new Phrase(
                    establecimiento.getTelefonoContacto() != null ? establecimiento.getTelefonoContacto() : "", dataFont));
                telefonoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                telefonoCell.setPadding(5);
                table.addCell(telefonoCell);

                PdfPCell correoCell = new PdfPCell(new Phrase(
                    establecimiento.getCorreoContacto() != null ? establecimiento.getCorreoContacto() : "", dataFont));
                correoCell.setPadding(5);
                table.addCell(correoCell);

                String horario = (establecimiento.getHorarioApertura() != null ? establecimiento.getHorarioApertura().toString() : "") +
                                " - " +
                                (establecimiento.getHorarioCierre() != null ? establecimiento.getHorarioCierre().toString() : "");
                PdfPCell horarioCell = new PdfPCell(new Phrase(horario, dataFont));
                horarioCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                horarioCell.setPadding(5);
                table.addCell(horarioCell);
            }

            document.add(table);
            document.close();

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_PDF);
            responseHeaders.setContentDispositionFormData("attachment", "establecimientos_admin.pdf");

            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(outputStream.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Export methods for Espacios Deportivos of a specific Establecimiento
    @GetMapping("/establecimientos/{id}/espacios/export/excel")
    public ResponseEntity<byte[]> exportarEspaciosEstablecimientoExcel(@PathVariable("id") Integer establecimientoId) {
        try {
            EstablecimientoDeportivo establecimiento = establecimientoDeportivoRepository.findById(establecimientoId)
                .orElseThrow(() -> new RuntimeException("Establecimiento no encontrado"));
            List<EspacioDeportivo> espacios = espacioDeportivoRepository.findAllByEstablecimientoDeportivo(establecimiento);

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Espacios Deportivos");

            // Estilo para el encabezado
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);

            // Estilo para las celdas de datos
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setAlignment(HorizontalAlignment.LEFT);

            // Crear encabezados
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Nombre", "Servicio Deportivo", "Estado", "Precio por Hora", "Descripción"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Llenar datos
            int rowNum = 1;
            for (EspacioDeportivo espacio : espacios) {
                Row row = sheet.createRow(rowNum++);

                Cell nombreCell = row.createCell(0);
                nombreCell.setCellValue(espacio.getNombre());
                nombreCell.setCellStyle(dataStyle);

                Cell servicioCell = row.createCell(1);
                servicioCell.setCellValue(espacio.getServicioDeportivo() != null ?
                    espacio.getServicioDeportivo().getServicioDeportivo() : "N/A");
                servicioCell.setCellStyle(dataStyle);

                Cell estadoCell = row.createCell(2);
                estadoCell.setCellValue(espacio.getEstadoServicio().name());
                estadoCell.setCellStyle(dataStyle);

                Cell precioCell = row.createCell(3);
                precioCell.setCellValue(espacio.getPrecioPorHora() != null ?
                    "S/ " + espacio.getPrecioPorHora().toString() : "N/A");
                precioCell.setCellStyle(dataStyle);

                Cell descripcionCell = row.createCell(4);
                descripcionCell.setCellValue(espacio.getDescripcion() != null ? espacio.getDescripcion() : "");
                descripcionCell.setCellStyle(dataStyle);
            }

            // Ajustar ancho de columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) < 3000) {
                    sheet.setColumnWidth(i, 3000);
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            responseHeaders.setContentDispositionFormData("attachment",
                "espacios_" + establecimiento.getEstablecimientoDeportivoNombre().replaceAll("[^a-zA-Z0-9]", "_") + ".xlsx");

            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(outputStream.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/establecimientos/{id}/espacios/export/pdf")
    public ResponseEntity<byte[]> exportarEspaciosEstablecimientoPdf(@PathVariable("id") Integer establecimientoId) {
        try {
            EstablecimientoDeportivo establecimiento = establecimientoDeportivoRepository.findById(establecimientoId)
                .orElseThrow(() -> new RuntimeException("Establecimiento no encontrado"));
            List<EspacioDeportivo> espacios = espacioDeportivoRepository.findAllByEstablecimientoDeportivo(establecimiento);

            Document document = new Document(PageSize.A4.rotate());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, outputStream);

            document.open();

            // Título
            com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
            Paragraph title = new Paragraph("Espacios Deportivos - " + establecimiento.getEstablecimientoDeportivoNombre(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Fecha de generación
            com.itextpdf.text.Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
            Paragraph dateGenerated = new Paragraph("Fecha de generación: " +
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), dateFont);
            dateGenerated.setAlignment(Element.ALIGN_CENTER);
            dateGenerated.setSpacingAfter(20);
            document.add(dateGenerated);

            // Crear tabla
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // Configurar anchos de columnas
            float[] columnWidths = {3f, 3f, 2f, 2f, 4f};
            table.setWidths(columnWidths);

            // Estilo para encabezados
            com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
            BaseColor headerColor = new BaseColor(52, 58, 64);

            // Agregar encabezados
            String[] headers = {"Nombre", "Servicio Deportivo", "Estado", "Precio por Hora", "Descripción"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(headerColor);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(8);
                table.addCell(cell);
            }

            // Estilo para datos
            com.itextpdf.text.Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

            // Agregar datos
            for (EspacioDeportivo espacio : espacios) {
                PdfPCell nombreCell = new PdfPCell(new Phrase(espacio.getNombre(), dataFont));
                nombreCell.setPadding(5);
                table.addCell(nombreCell);

                PdfPCell servicioCell = new PdfPCell(new Phrase(
                    espacio.getServicioDeportivo() != null ?
                    espacio.getServicioDeportivo().getServicioDeportivo() : "N/A", dataFont));
                servicioCell.setPadding(5);
                table.addCell(servicioCell);

                PdfPCell estadoCell = new PdfPCell(new Phrase(espacio.getEstadoServicio().name(), dataFont));
                estadoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                estadoCell.setPadding(5);
                table.addCell(estadoCell);

                PdfPCell precioCell = new PdfPCell(new Phrase(
                    espacio.getPrecioPorHora() != null ?
                    "S/ " + espacio.getPrecioPorHora().toString() : "N/A", dataFont));
                precioCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                precioCell.setPadding(5);
                table.addCell(precioCell);

                PdfPCell descripcionCell = new PdfPCell(new Phrase(
                    espacio.getDescripcion() != null ? espacio.getDescripcion() : "", dataFont));
                descripcionCell.setPadding(5);
                table.addCell(descripcionCell);
            }

            document.add(table);
            document.close();

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_PDF);
            responseHeaders.setContentDispositionFormData("attachment",
                "espacios_" + establecimiento.getEstablecimientoDeportivoNombre().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf");

            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(outputStream.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Helper methods for text conversion
    private String getEstadoObservacionTexto(Observacion.Estado estado) {
        switch (estado) {
            case pendiente:
                return "Pendiente";
            case en_proceso:
                return "En Proceso";
            case resuelto:
                return "Resuelto";
            default:
                return "Desconocido";
        }
    }

    private String getEstadoPagoTexto(Pago.EstadoTransaccion estado) {
        switch (estado) {
            case pendiente:
                return "Pendiente";
            case completado:
                return "Completado";
            case fallido:
                return "Fallido";
            default:
                return "Desconocido";
        }
    }


    // ---- Flujo reembolsos ----

    // Listar reembolsos pendientes
    @GetMapping("/reembolsos")
    public String listarReembolsos(Model model) {
        List<Reembolso> reembolsosPendientes = reembolsoRepository.findByEstadoOrderByFechaReembolsoDesc(Reembolso.Estado.pendiente);
        model.addAttribute("reembolsosPendientes", reembolsosPendientes);
        return "admin/reembolsosList";
    }

    // Ver detalles de un reembolso
    @GetMapping("/reembolsos/info")
    public String verInfoReembolso(@RequestParam("id") Integer id, Model model) {
        Reembolso reembolso = reembolsoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ID inválido"));
        model.addAttribute("reembolso", reembolso);
        return "admin/reembolsoInfo";
    }

    // Procesar reembolso (subir foto y marcar como completado)
    @PostMapping("/reembolsos/completar")
    public String completarReembolso(
            @RequestParam("id") Integer id,
            @RequestParam(value = "fotoComprobante", required = false) MultipartFile fotoComprobante,
            @RequestParam("detallesTransaccion") String detallesTransaccion,
            RedirectAttributes redirectAttributes) {
        Reembolso reembolso = reembolsoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ID inválido"));

        // Validar y subir la imagen al S3
        String defaultFotoUrl = "https://img.freepik.com/foto-gratis/disparo-cabeza-hombre-atractivo-sonriendo-complacido-mirando-intrigado-pie-sobre-fondo-azul_1258-65733.jpg";
        String existingFotoUrl = reembolso.getFotoComprobacionReembolsoUrl();
        if (fotoComprobante != null && !fotoComprobante.isEmpty()) {
            String contentType = fotoComprobante.getContentType();
            if (contentType == null || !contentType.matches("^(image/(jpeg|png|jpg))$")) {
                redirectAttributes.addFlashAttribute("message", "El archivo debe ser una imagen (JPEG, PNG o JPG)");
                redirectAttributes.addFlashAttribute("messageType", "error");
                return "redirect:/admin/reembolsos/info?id=" + id;
            }
            String uploadResult = s3Service.uploadFile(fotoComprobante);
            if (uploadResult != null && uploadResult.contains("URL:") && !uploadResult.trim().isEmpty()) {
                String fotoUrl = uploadResult.substring(uploadResult.indexOf("URL: ") + 5).trim();
                if (fotoUrl.length() > 255) {
                    redirectAttributes.addFlashAttribute("message", "La URL de la foto no puede superar los 255 caracteres");
                    redirectAttributes.addFlashAttribute("messageType", "error");
                    return "redirect:/admin/reembolsos/info?id=" + id;
                } else if (fotoUrl.isEmpty()) {
                    redirectAttributes.addFlashAttribute("message", "La URL generada está vacía");
                    redirectAttributes.addFlashAttribute("messageType", "error");
                    return "redirect:/admin/reembolsos/info?id=" + id;
                } else {
                    reembolso.setFotoComprobacionReembolsoUrl(fotoUrl);
                }
            } else {
                reembolso.setFotoComprobacionReembolsoUrl(existingFotoUrl != null && !existingFotoUrl.isEmpty() ? existingFotoUrl : defaultFotoUrl);
                redirectAttributes.addFlashAttribute("message", "Error al subir la foto: " + (uploadResult != null ? uploadResult : "Resultado inválido"));
                redirectAttributes.addFlashAttribute("messageType", "error");
                return "redirect:/admin/reembolsos/info?id=" + id;
            }
        } else {
            reembolso.setFotoComprobacionReembolsoUrl(existingFotoUrl != null && !existingFotoUrl.isEmpty() ? existingFotoUrl : defaultFotoUrl);
        }

        // Actualizar estado y detalles
        reembolso.setEstado(Reembolso.Estado.completado);
        reembolso.setDetallesTransaccion(detallesTransaccion);
        reembolso.setFechaReembolso(LocalDateTime.now());
        reembolsoRepository.save(reembolso);

        redirectAttributes.addFlashAttribute("msg", "Reembolso completado satisfactoriamente");
        return "redirect:/admin/reembolsos";
    }

}