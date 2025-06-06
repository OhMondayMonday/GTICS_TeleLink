package com.example.telelink.controller;

import com.example.telelink.dto.vecino.PagoRequest;
import com.example.telelink.entity.*;
import com.example.telelink.repository.*;
import com.example.telelink.service.S3Service;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.File;
import java.io.IOException;

import java.io.*;
import java.net.*;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;

import com.example.telelink.dto.ReservaCalendarioDTO;
import com.example.telelink.entity.EspacioDeportivo;
import com.example.telelink.repository.EspacioDeportivoRepository;
import com.example.telelink.repository.ReservaRepository;
import com.example.telelink.repository.UsuarioRepository;
import com.example.telelink.entity.Reserva;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired

    private ReservaRepository reservaRepository;

    @Autowired
    private EspacioDeportivoRepository espacioDeportivoRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private AvisoRepository avisoRepository;

    @Autowired
    private MetodoPagoRepository metodoPagoRepository;

    @Autowired
    private S3Service s3Service;


    public class EspacioDeportivoConRatingDTO {
        private EspacioDeportivo espacioDeportivo;
        private double promedioCalificacion;
        private long reviewCount;

        public EspacioDeportivoConRatingDTO(EspacioDeportivo espacioDeportivo, double promedioCalificacion, long reviewCount) {
            this.espacioDeportivo = espacioDeportivo;
            this.promedioCalificacion = promedioCalificacion;
            this.reviewCount = reviewCount;
        }

        public EspacioDeportivo getEspacioDeportivo() {
            return espacioDeportivo;
        }

        public double getPromedioCalificacion() {
            return promedioCalificacion;
        }

        public long getReviewCount() {
            return reviewCount;
        }
    }

    @GetMapping("/inicio")
    public String mostrarInicio(Model model, HttpSession session) {
        // Buscar usuario ID 6 (usuario por defecto)
        Usuario usuario = (Usuario) session.getAttribute("usuario");

        // Obtener el aviso más reciente
        Aviso ultimoAviso = avisoRepository.findLatestAviso();

        // Obtener las 3 canchas más populares ordenadas por promedio de calificación
        List<Object[]> resultados = espacioDeportivoRepository.findTop3ByEstadoServicioOrderByAverageRatingDesc(
                EspacioDeportivo.EstadoServicio.operativo);
        List<EspacioDeportivoConRatingDTO> canchasPopulares = resultados.stream()
                .map(result -> new EspacioDeportivoConRatingDTO(
                        (EspacioDeportivo) result[0],
                        ((Number) result[1]).doubleValue(),
                        ((Number) result[2]).longValue()
                ))
                .collect(Collectors.toList());

        // Pasar datos al modelo
        model.addAttribute("usuario", usuario);
        model.addAttribute("activeItem", "inicio");
        model.addAttribute("ultimoAviso", ultimoAviso);
        model.addAttribute("canchasPopulares", canchasPopulares);
        return "vecino/vecino-index";
    }

    @GetMapping("/reservas/{id}")
    public String mostrarReservation(@PathVariable Integer id, Model model, HttpSession session) {
        EspacioDeportivo espacio = espacioDeportivoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el espacio deportivo"));

        model.addAttribute("espacio", espacio);

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/usuarios/inicio";
        }
        model.addAttribute("usuario", usuario);

        return "vecino/vecino-servicioDeportivo";
    }


    @GetMapping("/reservas/futbol")
    public String mostrarFutbolReservation(Model model) {
        // Aquí puedes agregar cualquier lógica que necesites
        return "vecino/vecino-futbol";
    }

    @GetMapping("/reservas/piscina")
    public String mostrarPiscinaReservation(Model model) {
        // Aquí puedes agregar cualquier lógica que necesites
        return "vecino/vecino-piscina";
    }

    @GetMapping("/reservas/multiple")
    public String mostrarMultipleReservation(Model model) {
        // Aquí puedes agregar cualquier lógica que necesites
        return "vecino/vecino-multiple";
    }

    @GetMapping("/reservas/gym")
    public String mostrarGymReservation(Model model) {
        // Aquí puedes agregar cualquier lógica que necesites
        return "vecino/vecino-gym";
    }

    @GetMapping("/perfil")
    public String mostrarPerfil(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        model.addAttribute("usuario", usuario);
        return "vecino/vecino-perfil";
    }

    @GetMapping("/editar-perfil")
    public String mostrarEditarPerfil(@ModelAttribute("usuario") Usuario usuarioActualizado, Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        model.addAttribute("usuario", usuario);
        return "vecino/vecino-editarPerfil";
    }

    @PostMapping("/actualizar-perfil")
    public String actualizarPerfil(
            @Valid @ModelAttribute("usuario") Usuario usuarioActualizado,
            BindingResult result,
            @RequestParam("fotoPerfil") MultipartFile fotoPerfil,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");

        // Validar el formato de la foto si se subió una y asociar el error a fotoPerfilUrl
        if (fotoPerfil != null && !fotoPerfil.isEmpty()) {
            String contentType = fotoPerfil.getContentType();
            if (contentType == null || !contentType.matches("^(image/(jpeg|png|jpg))$")) {
                result.rejectValue("fotoPerfilUrl", "typeMismatch", "El archivo debe ser una imagen (JPEG, PNG o JPG)");
            }
        }

        // Si hay errores de validación, retornar directamente la vista con los errores
        if (result.hasErrors()) {
            result.getAllErrors().forEach(error -> {
                System.out.println("Error de validación: " + error.getDefaultMessage());
                if (error instanceof FieldError) {
                    FieldError fieldError = (FieldError) error;
                    System.out.println("Campo: " + fieldError.getField());
                }
            });
            model.addAttribute("usuario", usuario);
            model.addAttribute("org.springframework.validation.BindingResult.usuario", result);
            return "vecino/vecino-editarPerfil";
        }

        // Actualizar los campos permitidos (teléfono)
        usuario.setTelefono(usuarioActualizado.getTelefono());

        // Manejar la subida de la foto al bucket S3
        String defaultFotoPerfilUrl = usuario.getFotoPerfilUrl() != null ? usuario.getFotoPerfilUrl() : "https://img.freepik.com/foto-gratis/disparo-cabeza-hombre-atractivo-sonriendo-complacido-mirando-intrigado-pie-sobre-fondo-azul_1258-65733.jpg";
        if (fotoPerfil != null && !fotoPerfil.isEmpty()) {
            System.out.println("Intentando subir archivo a S3...");
            String uploadResult = s3Service.uploadFile(fotoPerfil);
            System.out.println("Resultado de la subida: " + uploadResult);

            if (uploadResult != null && uploadResult.contains("URL:")) {
                // Extraer la URL si la subida fue exitosa
                String fotoPerfilUrl = uploadResult.substring(uploadResult.indexOf("URL: ") + 5).trim();
                usuario.setFotoPerfilUrl(fotoPerfilUrl);
                redirectAttributes.addFlashAttribute("message", "Foto de perfil subida exitosamente.");
                redirectAttributes.addFlashAttribute("messageType", "success");
            } else if (uploadResult != null && uploadResult.contains("Error:")) {
                // Manejar el caso de error (incluyendo ExpiredToken)
                System.out.println("Error detectado en el resultado: " + uploadResult);
                usuario.setFotoPerfilUrl(defaultFotoPerfilUrl);
                redirectAttributes.addFlashAttribute("message", "Error al subir la foto de perfil: " + uploadResult + ". Se usó una imagen por defecto.");
                redirectAttributes.addFlashAttribute("messageType", "error");
            } else {
                // Caso inesperado
                System.out.println("Resultado inválido de la subida: " + uploadResult);
                usuario.setFotoPerfilUrl(defaultFotoPerfilUrl);
                redirectAttributes.addFlashAttribute("message", "Error desconocido al subir la foto. Se usó una imagen por defecto.");
                redirectAttributes.addFlashAttribute("messageType", "error");
            }
        } else {
            usuario.setFotoPerfilUrl(defaultFotoPerfilUrl);
        }

        // Guardar los cambios en la base de datos
        usuarioRepository.save(usuario);

        // Actualizar el objeto en la sesión
        session.setAttribute("usuario", usuario);

        return "redirect:/usuarios/perfil";
    }

    @GetMapping("")
    public String listarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioRepository.findAllByOrderByUsuarioIdAsc());
        return "lista-usuarios";
    }


    @GetMapping("/pagos")
    public String mostrarPagos(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/usuarios/inicio";
        }

        List<Pago> pagos = pagoRepository.findByReserva_Usuario(usuario);

        model.addAttribute("usuario", usuario);
        model.addAttribute("pagos", pagos); // importante para mostrarlos en la vista
        model.addAttribute("activeItem", "pagos");

        return "vecino/vecino-pago";
    }

    /*
    @GetMapping("/mis-reservas")
    public String mostrarReservas(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/usuarios/inicio";
        }
        List<Reserva> reservas = reservaRepository.findByUsuarioOrderByInicioReservaDesc(usuario);
        // Create a list of maps to include canCancel flag
        List<Map<String, Object>> reservasWithCancelFlag = reservas.stream().map(reserva -> {
            Map<String, Object> reservaMap = new HashMap<>();
            reservaMap.put("reserva", reserva);
            reservaMap.put("canCancel", LocalDateTime.now().isBefore(reserva.getInicioReserva().minusHours(48))
                    && !reserva.getEstado().name().equals("cancelada"));
            return reservaMap;
        }).collect(Collectors.toList());

        model.addAttribute("usuario", usuario);
        model.addAttribute("reservasWithCancelFlag", reservasWithCancelFlag);
        model.addAttribute("activeItem", "reservas");
        return "vecino/vecino-mis-reservas";
    }*/
    @GetMapping("/mis-reservas")
    public String mostrarReservas(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/usuarios/inicio";
        }
        List<Reserva> reservas = reservaRepository.findByUsuarioOrderByInicioReservaDesc(usuario);
        List<Map<String, Object>> reservasWithCancelFlag = reservas.stream().map(reserva -> {
            Map<String, Object> reservaMap = new HashMap<>();
            reservaMap.put("reserva", reserva);
            reservaMap.put("canCancel", LocalDateTime.now().isBefore(reserva.getInicioReserva().minusHours(48))
                    && !reserva.getEstado().name().equals("cancelada"));
            // Calculate duration in hours
            long duracionHoras = Duration.between(reserva.getInicioReserva(), reserva.getFinReserva()).toHours();
            reservaMap.put("duracionHoras", duracionHoras);
            // Calculate total price
            BigDecimal precioTotal = reserva.getEspacioDeportivo().getPrecioPorHora().multiply(BigDecimal.valueOf(duracionHoras));
            reservaMap.put("precioTotal", precioTotal);
            return reservaMap;
        }).collect(Collectors.toList());

        model.addAttribute("usuario", usuario);
        model.addAttribute("reservasWithCancelFlag", reservasWithCancelFlag);
        model.addAttribute("activeItem", "reservas");
        return "vecino/vecino-mis-reservas";
    }

    @PostMapping("/reserva/cancelar/{id}")
    public String cancelarReserva(@PathVariable("id") Integer id,
                                  @RequestParam(required = false) String razon,
                                  RedirectAttributes redirectAttributes,
                                  HttpSession session) {

        // Obtener el usuario actual de la sesión
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/usuarios/inicio";
        }

        // Buscar la reserva por ID
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));

        // Verificar que la reserva pertenezca al usuario actual
        if (!reserva.getUsuario().getUsuarioId().equals(usuario.getUsuarioId())) {
            redirectAttributes.addFlashAttribute("error", "No tienes permiso para cancelar esta reserva");
            return "redirect:/usuarios/mis-reservas";
        }

        // Verificar si la cancelación es con menos de 48 horas de anticipación
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime limite = reserva.getInicioReserva().minusHours(48);

        boolean conPenalidad = ahora.isAfter(limite);

        // Cambiar el estado de la reserva
        reserva.setEstado(Reserva.Estado.cancelada);
        reserva.setRazonCancelacion(razon);
        reserva.setFechaActualizacion(LocalDateTime.now());
        reservaRepository.save(reserva);

        // Buscar si existe un pago asociado a esta reserva
        Optional<Pago> pagoOptional = pagoRepository.findByReserva(reserva);

        // Mensaje para el usuario
        String mensaje;
        if (conPenalidad) {
            mensaje = "Reserva cancelada. Se ha aplicado la penalidad por cancelación con menos de 48 horas de anticipación.";
        } else {
            mensaje = "Reserva cancelada correctamente sin penalidad.";

            // Si hay un pago y no hay penalidad, podríamos marcar el pago como reembolsable
            if (pagoOptional.isPresent()) {
                Pago pago = pagoOptional.get();
                // Aquí podrías implementar la lógica de reembolso
                // pago.setEstado("reembolsado");
                // pagoRepository.save(pago);
            }
        }

        redirectAttributes.addFlashAttribute("success", mensaje);
        return "redirect:/usuarios/mis-reservas";
    }

    @GetMapping("/cancha")
    public String mostrarCancha(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/usuarios/inicio";
        }

        List<EspacioDeportivo> espacios = espacioDeportivoRepository.findAll();

        model.addAttribute("usuario", usuario);
        model.addAttribute("espacios", espacios);
        model.addAttribute("activeItem", "canchas");
        return "vecino/vecino-cancha";
    }

    // Endpoint para obtener espacios filtrados via AJAX
    @GetMapping("/espacios")
    @ResponseBody
    public ResponseEntity<List<EspacioDeportivo>> obtenerEspacios(
            @RequestParam(required = false) String servicios,
            @RequestParam(required = false) BigDecimal precioMaximo,
            @RequestParam(required = false) String zonas,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String busqueda) {

        List<EspacioDeportivo> espacios = espacioDeportivoRepository.findAll();

        // Filtrar por servicios deportivos
        if (servicios != null && !servicios.isEmpty()) {
            String[] tiposServicios = servicios.split(",");
            espacios = espacios.stream()
                    .filter(espacio -> {
                        String tipoServicio = espacio.getServicioDeportivo().getServicioDeportivo();
                        for (String tipo : tiposServicios) {
                            if (coincideTipoServicio(tipoServicio, tipo.trim())) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
        }

        // Filtrar por precio
        if (precioMaximo != null) {
            espacios = espacios.stream()
                    .filter(espacio -> espacio.getPrecioPorHora().compareTo(precioMaximo) <= 0)
                    .collect(Collectors.toList());
        }

        // Filtrar por zonas (basado en la dirección del establecimiento)
        if (zonas != null && !zonas.isEmpty()) {
            String[] zonasArray = zonas.split(",");
            espacios = espacios.stream()
                    .filter(espacio -> {
                        String direccion = espacio.getEstablecimientoDeportivo().getDireccion().toLowerCase();
                        for (String zona : zonasArray) {
                            if (coincideZona(direccion, zona.trim())) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
        }

        // Filtrar por búsqueda de texto
        if (busqueda != null && !busqueda.trim().isEmpty()) {
            String busquedaLower = busqueda.toLowerCase().trim();
            espacios = espacios.stream()
                    .filter(espacio ->
                            espacio.getNombre().toLowerCase().contains(busquedaLower) ||
                                    (espacio.getDescripcion() != null && espacio.getDescripcion().toLowerCase().contains(busquedaLower)) ||
                                    espacio.getServicioDeportivo().getServicioDeportivo().toLowerCase().contains(busquedaLower) ||
                                    espacio.getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre().toLowerCase().contains(busquedaLower)
                    )
                    .collect(Collectors.toList());
        }

        // Filtrar solo espacios operativos
        espacios = espacios.stream()
                .filter(espacio -> espacio.getEstadoServicio() == EspacioDeportivo.EstadoServicio.operativo)
                .collect(Collectors.toList());

        return ResponseEntity.ok(espacios);
    }

    // Método auxiliar para verificar coincidencia de tipo de servicio
    private boolean coincideTipoServicio(String tipoServicio, String filtro) {
        switch (filtro) {
            case "futbol-loza":
                return tipoServicio.toLowerCase().contains("fútbol") &&
                        tipoServicio.toLowerCase().contains("loza");
            case "futbol-grass":
                return tipoServicio.toLowerCase().contains("fútbol") &&
                        (tipoServicio.toLowerCase().contains("grass") ||
                                tipoServicio.toLowerCase().contains("sintético"));
            case "gimnasio":
                return tipoServicio.toLowerCase().contains("gimnasio");
            case "piscina":
                return tipoServicio.toLowerCase().contains("piscina");
            case "atletismo":
                return tipoServicio.toLowerCase().contains("atletismo") ||
                        tipoServicio.toLowerCase().contains("pista");
            case "multiusos":
                return tipoServicio.toLowerCase().contains("multiusos") ||
                        tipoServicio.toLowerCase().contains("multipropósito") ||
                        tipoServicio.toLowerCase().contains("básquet") ||
                        tipoServicio.toLowerCase().contains("vóley");
            default:
                return false;
        }
    }

    // Método auxiliar para verificar coincidencia de zona
    private boolean coincideZona(String direccion, String zona) {
        switch (zona) {
            case "zona-costado":
                return direccion.contains("upc");
            case "zona-avla-marina":
                return direccion.contains("marina");
            case "zona-parque-zonal":
                return direccion.contains("parque") && direccion.contains("zonal");
            case "zona-plaza-sanmiguel":
                return direccion.contains("plaza") && direccion.contains("san miguel");
            case "zona-univcatolica":
                return direccion.contains("pucp") || direccion.contains("católica");
            default:
                return false;
        }
    }

    @GetMapping("/ayuda")
    public String mostrarAyuda(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/usuarios/inicio";
        }
        model.addAttribute("usuario", usuario);
        model.addAttribute("activeItem", "ayuda");
        return "vecino/vecino-ayuda";
    }

    @GetMapping("/calendario")
    public String mostrarCalendario(Model model) {
        // Aquí puedes agregar cualquier lógica que necesites
        return "vecino/vecino-calendario";
    }

    @GetMapping("/reservar/{espacioId}")
    public String mostrarReservar(@PathVariable("espacioId") Integer espacioId,
                                  @RequestParam(value = "inicio", required = false) String inicio,
                                  @RequestParam(value = "fin", required = false) String fin,
                                  Model model, HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/usuarios/inicio";
        }

        List<Reserva> reservasUsuario = reservaRepository.findByUsuario_UsuarioId(usuario.getUsuarioId());
        List<EspacioDeportivo> espacioDeportivos = espacioDeportivoRepository.findAll();

        model.addAttribute("usuario", usuario);
        model.addAttribute("reservas", reservasUsuario);
        model.addAttribute("espacios", espacioDeportivos);
        model.addAttribute("espacioSeleccionadoId", espacioId);

        if (inicio != null && fin != null) {
            try {
                OffsetDateTime odtInicio = OffsetDateTime.parse(inicio);
                OffsetDateTime odtFin = OffsetDateTime.parse(fin);

                ZoneId zonaLocal = ZoneId.of("America/Lima");
                LocalDateTime inicioLDT = odtInicio.atZoneSameInstant(zonaLocal).toLocalDateTime();
                LocalDateTime finLDT = odtFin.atZoneSameInstant(zonaLocal).toLocalDateTime();

                // Verificar si ya hay una reserva que se superponga
                boolean conflicto = reservaRepository.existsByEspacioDeportivo_EspacioDeportivoIdAndInicioReservaLessThanAndFinReservaGreaterThan(
                        espacioId, finLDT, inicioLDT);

                if (conflicto) {
                    redirectAttributes.addFlashAttribute("error", "Ya existe una reserva en este horario.");
                    return "redirect:/usuarios/calendario/" + espacioId;
                }

                // Crear reserva en estado pendiente
                EspacioDeportivo espacio = espacioDeportivoRepository.findById(espacioId)
                        .orElseThrow(() -> new RuntimeException("Espacio no encontrado"));

                Reserva reserva = new Reserva();
                reserva.setUsuario(usuario);
                reserva.setEspacioDeportivo(espacio);
                reserva.setInicioReserva(inicioLDT);
                reserva.setFinReserva(finLDT);
                reserva.setEstado(Reserva.Estado.pendiente);
                reserva.setFechaCreacion(LocalDateTime.now());

                // Calcular duración y precio antes de redirigir
                long duracionHoras = Duration.between(inicioLDT, finLDT).toHours();
                BigDecimal precioPorHora = espacio.getPrecioPorHora();
                BigDecimal precioTotal = precioPorHora.multiply(BigDecimal.valueOf(duracionHoras));

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String inicioFormateado = inicioLDT.format(formatter);
                String finFormateado = finLDT.format(formatter);

                redirectAttributes.addFlashAttribute("inicioFormateado", inicioFormateado);
                redirectAttributes.addFlashAttribute("finFormateado", finFormateado);
                redirectAttributes.addFlashAttribute("precioTotalReserva", precioTotal);
                redirectAttributes.addFlashAttribute("duracionHoras", duracionHoras);


                reservaRepository.save(reserva);

                redirectAttributes.addFlashAttribute("mensaje", "Reserva pendiente registrada correctamente.");
                return "redirect:/usuarios/reservar/" + espacioId;

            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Error al procesar la reserva: " + e.getMessage());
                return "redirect:/usuarios/reservar/" + espacioId;
            }
        }

        return "vecino/vecino-reservar";
    }

    @PostMapping("/confirmar-reserva{espacioId}")
    public String confirmarReserva(
            @PathVariable("espacioId") Integer espacioId,
            @RequestParam("fechaInicio") String fechaInicio,
            @RequestParam("fechaFin") String fechaFin,
            @RequestParam(value = "numeroCarrilPiscina", required = false) Integer numeroCarrilPiscina,
            @RequestParam(value = "numeroCarrilPista", required = false) Integer numeroCarrilPista,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            // Obtener el usuario actual
            String nombres = principal.getName();
            Usuario usuario = usuarioRepository.findByNombres(nombres);

            // Obtener el espacio deportivo
            EspacioDeportivo espacio = espacioDeportivoRepository.findById(espacioId)
                    .orElseThrow(() -> new RuntimeException("Espacio deportivo no encontrado"));

            // Convertir las fechas de String a LocalDateTime
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            LocalDateTime inicioReserva = LocalDateTime.parse(fechaInicio, formatter);
            LocalDateTime finReserva = LocalDateTime.parse(fechaFin, formatter);

            // Crear la reserva
            Reserva reserva = new Reserva();
            reserva.setUsuario(usuario);
            reserva.setEspacioDeportivo(espacio);
            reserva.setInicioReserva(inicioReserva);
            reserva.setFinReserva(finReserva);
            reserva.setNumeroCarrilPiscina(numeroCarrilPiscina);
            reserva.setNumeroCarrilPista(numeroCarrilPista);
            reserva.setEstado(Reserva.Estado.pendiente);
            reserva.setFechaCreacion(LocalDateTime.now());

            // Guardar la reserva
            reservaRepository.save(reserva);

            redirectAttributes.addFlashAttribute("mensaje", "Reserva realizada con éxito");
            return "redirect:/usuarios/mis-reservas";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al procesar la reserva: " + e.getMessage());
            return "redirect:/usuarios/reservar";
        }
    }

    // Metodo auxiliar para guardar el comprobante
    private String guardarComprobante(MultipartFile comprobante, Long usuarioId) throws IOException {
        // Crear directorio si no existe
        String directorioComprobantes = "uploads/comprobantes/" + usuarioId;
        File directorio = new File(directorioComprobantes);
        if (!directorio.exists()) {
            directorio.mkdirs();
        }

        // Generar nombre único para el archivo
        String nombreArchivo = System.currentTimeMillis() + "_" + comprobante.getOriginalFilename();
        String rutaCompleta = directorioComprobantes + "/" + nombreArchivo;

        // Guardar archivo
        File archivoDestino = new File(rutaCompleta);
        comprobante.transferTo(archivoDestino);

        return rutaCompleta;
    }

    @PostMapping("/crear-link-pago")
    public ResponseEntity<String> crearLinkPago(@RequestBody PagoRequest pagoRequest) throws IOException {
        String apiKey = "TU_API_KEY_IZZIPAY";
        String url = "https://api.izzypay.pe/api/v1/checkout/create";
        String body = "{"
                + "\"amount\":" + pagoRequest.getMonto() + ","
                + "\"currency\":\"PEN\","
                + "\"description\":\"" + pagoRequest.getDescripcion() + "\","
                + "\"customer_email\":\"" + pagoRequest.getEmail() + "\""
                + "}";

        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Authorization", "Bearer " + apiKey);
        con.setDoOutput(true);
        con.getOutputStream().write(body.getBytes("UTF-8"));

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // Extrae el link de pago de la respuesta JSON (usa una librería JSON en producción)
        String linkPago = response.toString().split("\"checkout_url\":\"")[1].split("\"")[0].replace("\\/", "/");
        return ResponseEntity.ok(linkPago);
    }

    @GetMapping("/api/espacios")
    @ResponseBody
    public List<?> obtenerEspacios() {
        return espacioDeportivoRepository.findAll().stream()
                .map(e -> new Object() {
                    public final String id = e.getEspacioDeportivoId().toString();
                    public final String title = e.getNombre();
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/api/reservas")
    @ResponseBody
    public List<ReservaCalendarioDTO> obtenerReservas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        if (fecha == null) fecha = LocalDate.now();
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = inicio.plusDays(1);

        return reservaRepository.findByInicioReservaBetween(inicio, fin).stream()
                .map(r -> new ReservaCalendarioDTO(
                        r.getEspacioDeportivo().getEspacioDeportivoId().toString(),
                        "Reservado",
                        r.getInicioReserva().toString(),
                        r.getFinReserva().toString()
                )).collect(Collectors.toList());
    }    @GetMapping("/reservasCalendario/{id}")
    public String verCalendarioReservas(@PathVariable("id") Integer id, Model model) {
        // Buscar el espacio deportivo por ID
        EspacioDeportivo espacioDeportivo = espacioDeportivoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Espacio deportivo no encontrado"));
        
        // Pasar el objeto completo y el ID al modelo
        model.addAttribute("espacioId", id);
        model.addAttribute("espacio", espacioDeportivo);
        
        return "vecino/reservas-futbol-calendario";
    }

    // --- FLUJO DE PAGO DE RESERVA ---
    /*
    @GetMapping("/pagar/{reservaId}")
    public String mostrarPagoReserva(@PathVariable("reservaId") Integer reservaId, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/usuarios/inicio";
        }
        Optional<Reserva> optReserva = reservaRepository.findById(reservaId);
        if (optReserva.isEmpty() || !optReserva.get().getUsuario().getUsuarioId().equals(usuario.getUsuarioId())) {
            redirectAttributes.addFlashAttribute("error", "Reserva no encontrada o no autorizada");
            return "redirect:/usuarios/mis-reservas";
        }
        Reserva reserva = optReserva.get();
        if (!reserva.getEstado().equals(Reserva.Estado.pendiente)) {
            redirectAttributes.addFlashAttribute("error", "La reserva no está pendiente de pago");
            return "redirect:/usuarios/mis-reservas";
        }
        model.addAttribute("reserva", reserva);
        return "vecino/vecino-reservar";
    }

    @PostMapping("/pagar/{reservaId}")
    public String procesarMetodoPago(@PathVariable("reservaId") Integer reservaId,
                                     @RequestParam("metodoPagoId") Integer metodoPagoId,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/usuarios/inicio";
        }
        Optional<Reserva> optReserva = reservaRepository.findById(reservaId);
        if (optReserva.isEmpty() || !optReserva.get().getUsuario().getUsuarioId().equals(usuario.getUsuarioId())) {
            redirectAttributes.addFlashAttribute("error", "Reserva no encontrada o no autorizada");
            return "redirect:/usuarios/mis-reservas";
        }
        if (metodoPagoId == 1) {
            // Tarjeta/Izipay
            return "redirect:/usuarios/pago-tarjeta/" + reservaId;
        } else if (metodoPagoId == 2) {
            // Depósito
            return "redirect:/usuarios/pago-deposito/" + reservaId;
        } else {
            redirectAttributes.addFlashAttribute("error", "Método de pago no válido");
            return "redirect:/usuarios/pagar/" + reservaId;
        }
    }

    @GetMapping("/pago-tarjeta/{reservaId}")
    public String mostrarPagoTarjeta(@PathVariable("reservaId") Integer reservaId, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/usuarios/inicio";
        }
        Optional<Reserva> optReserva = reservaRepository.findById(reservaId);
        if (optReserva.isEmpty() || !optReserva.get().getUsuario().getUsuarioId().equals(usuario.getUsuarioId())) {
            redirectAttributes.addFlashAttribute("error", "Reserva no encontrada o no autorizada");
            return "redirect:/usuarios/mis-reservas";
        }
        Reserva reserva = optReserva.get();
        model.addAttribute("reserva", reserva);
        return "vecino/vecino-pago-tarjeta";
    }

    @PostMapping("/pago-tarjeta/{reservaId}")
    public String procesarPagoTarjeta(@PathVariable("reservaId") Integer reservaId,
                                      @RequestParam("numeroTarjeta") String numeroTarjeta,
                                      @RequestParam("nombreTitular") String nombreTitular,
                                      @RequestParam("fechaExpiracion") String fechaExpiracion,
                                      @RequestParam("cvv") String cvv,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/usuarios/inicio";
        }
        Optional<Reserva> optReserva = reservaRepository.findById(reservaId);
        if (optReserva.isEmpty() || !optReserva.get().getUsuario().getUsuarioId().equals(usuario.getUsuarioId())) {
            redirectAttributes.addFlashAttribute("error", "Reserva no encontrada o no autorizada");
            return "redirect:/usuarios/mis-reservas";
        }
        Reserva reserva = optReserva.get();
        // Validación simple de tarjeta (simulada)
        boolean tarjetaValida = numeroTarjeta != null && numeroTarjeta.matches("\\d{16}");
        String estadoTransaccion;
        if (tarjetaValida) {
            estadoTransaccion = "completado";
            reserva.setEstado(Reserva.Estado.confirmada);
        } else {
            estadoTransaccion = "fallido";
        }
        MetodoPago metodoPago = new MetodoPago();
        metodoPago.setMetodoPagoId(1); // Tarjeta/Izipay
        Pago pago = new Pago();
        pago.setReserva(reserva);
        pago.setMetodoPago(metodoPago);
        pago.setFechaPago(LocalDateTime.now());
        pago.setEstadoTransaccion(Pago.EstadoTransaccion.valueOf(estadoTransaccion));
        pagoRepository.save(pago);
        reservaRepository.save(reserva);
        if (tarjetaValida) {
            redirectAttributes.addFlashAttribute("mensaje", "Pago realizado con éxito");
        } else {
            redirectAttributes.addFlashAttribute("error", "Pago rechazado. Verifique los datos de la tarjeta.");
        }
        return "redirect:/usuarios/mis-reservas";
    }

    @GetMapping("/pago-deposito/{reservaId}")
    public String mostrarPagoDeposito(@PathVariable("reservaId") Integer reservaId, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/usuarios/inicio";
        }
        Optional<Reserva> optReserva = reservaRepository.findById(reservaId);
        if (optReserva.isEmpty() || !optReserva.get().getUsuario().getUsuarioId().equals(usuario.getUsuarioId())) {
            redirectAttributes.addFlashAttribute("error", "Reserva no encontrada o no autorizada");
            return "redirect:/usuarios/mis-reservas";
        }
        Reserva reserva = optReserva.get();
        model.addAttribute("reserva", reserva);
        return "vecino/vecino-pago-deposito";
    }

    @PostMapping("/pago-deposito/{reservaId}")
    public String procesarPagoDeposito(@PathVariable("reservaId") Integer reservaId,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/usuarios/inicio";
        }
        Optional<Reserva> optReserva = reservaRepository.findById(reservaId);
        if (optReserva.isEmpty() || !optReserva.get().getUsuario().getUsuarioId().equals(usuario.getUsuarioId())) {
            redirectAttributes.addFlashAttribute("error", "Reserva no encontrada o no autorizada");
            return "redirect:/usuarios/mis-reservas";
        }
        Reserva reserva = optReserva.get();
        MetodoPago metodoPago = new MetodoPago();
        metodoPago.setMetodoPagoId(2); // Depósito
        Pago pago = new Pago();
        pago.setReserva(reserva);
        pago.setMetodoPago(metodoPago);
        pago.setFechaPago(LocalDateTime.now());
        pago.setEstadoTransaccion(Pago.EstadoTransaccion.pendiente);
        pagoRepository.save(pago);
        // La reserva sigue pendiente hasta validación manual
        redirectAttributes.addFlashAttribute("mensaje", "Pago por depósito registrado. Pendiente de validación.");
        return "redirect:/usuarios/mis-reservas";
    }
     */

    // Mostrar formulario de selección de método de pago
    @GetMapping("/pagar/{reservaId}")
    public String mostrarPagoReserva(@PathVariable("reservaId") Integer reservaId, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            redirectAttributes.addFlashAttribute("error", "Por favor, inicia sesión para pagar.");
            return "redirect:/usuarios/inicio";
        }

        Optional<Reserva> optReserva = reservaRepository.findById(reservaId);
        if (optReserva.isEmpty() || !optReserva.get().getUsuario().getUsuarioId().equals(usuario.getUsuarioId())) {
            redirectAttributes.addFlashAttribute("error", "Reserva no encontrada o no autorizada");
            return "redirect:/usuarios/mis-reservas";
        }

        Reserva reserva = optReserva.get();
        if (!reserva.getEstado().equals(Reserva.Estado.pendiente)) {
            redirectAttributes.addFlashAttribute("error", "La reserva no está pendiente de pago");
            return "redirect:/usuarios/mis-reservas";
        }

        Optional<Pago> optPago = pagoRepository.findByReserva(reserva);
        if (optPago.isPresent() && optPago.get().getEstadoTransaccion().equals(Pago.EstadoTransaccion.completado)) {
            redirectAttributes.addFlashAttribute("error", "La reserva ya ha sido pagada");
            return "redirect:/usuarios/mis-reservas";
        }

        model.addAttribute("reserva", reserva);
        model.addAttribute("activeItem", "reservas");
        return "vecino/vecino-pagar";
    }

    // Procesar selección de método de pago
    @PostMapping("/pagar/{reservaId}")
    public String procesarMetodoPago(@PathVariable("reservaId") Integer reservaId,
                                     @RequestParam("metodoPagoId") Integer metodoPagoId,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            redirectAttributes.addFlashAttribute("error", "Por favor, inicia sesión para pagar.");
            return "redirect:/usuarios/inicio";
        }

        Optional<Reserva> optReserva = reservaRepository.findById(reservaId);
        if (optReserva.isEmpty() || !optReserva.get().getUsuario().getUsuarioId().equals(usuario.getUsuarioId())) {
            redirectAttributes.addFlashAttribute("error", "Reserva no encontrada o no autorizada");
            return "redirect:/usuarios/mis-reservas";
        }

        if (metodoPagoId == 1) {
            return "redirect:/usuarios/pago-tarjeta/" + reservaId;
        } else if (metodoPagoId == 2) {
            return "redirect:/usuarios/pago-deposito/" + reservaId;
        } else {
            redirectAttributes.addFlashAttribute("error", "Método de pago no válido");
            return "redirect:/usuarios/pagar/" + reservaId;
        }
    }

    // Mostrar formulario de pago con tarjeta
    @GetMapping("/pago-tarjeta/{reservaId}")
    public String mostrarPagoTarjeta(@PathVariable("reservaId") Integer reservaId, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            redirectAttributes.addFlashAttribute("error", "Por favor, inicia sesión para pagar.");
            return "redirect:/usuarios/inicio";
        }

        Optional<Reserva> optReserva = reservaRepository.findById(reservaId);
        if (optReserva.isEmpty() || !optReserva.get().getUsuario().getUsuarioId().equals(usuario.getUsuarioId())) {
            redirectAttributes.addFlashAttribute("error", "Reserva no encontrada o no autorizada");
            return "redirect:/usuarios/mis-reservas";
        }

        Reserva reserva = optReserva.get();
        if (!reserva.getEstado().equals(Reserva.Estado.pendiente)) {
            redirectAttributes.addFlashAttribute("error", "La reserva no está pendiente de pago");
            return "redirect:/usuarios/mis-reservas";
        }

        model.addAttribute("reserva", reserva);
        model.addAttribute("activeItem", "reservas");
        return "vecino/vecino-pago-tarjeta";
    }

    // Procesar pago con tarjeta (simulado)
    @PostMapping("/pago-tarjeta/{reservaId}")
    public String procesarPagoTarjeta(@PathVariable("reservaId") Integer reservaId,
                                      @RequestParam("numeroTarjeta") String numeroTarjeta,
                                      @RequestParam("nombreTitular") String nombreTitular,
                                      @RequestParam("fechaExpiracion") String fechaExpiracion,
                                      @RequestParam("cvv") String cvv,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            redirectAttributes.addFlashAttribute("error", "Por favor, inicia sesión para pagar.");
            return "redirect:/usuarios/inicio";
        }

        Optional<Reserva> optReserva = reservaRepository.findById(reservaId);
        if (optReserva.isEmpty() || !optReserva.get().getUsuario().getUsuarioId().equals(usuario.getUsuarioId())) {
            redirectAttributes.addFlashAttribute("error", "Reserva no encontrada o no autorizada");
            return "redirect:/usuarios/mis-reservas";
        }

        Reserva reserva = optReserva.get();
        // Validaciones de tarjeta
        boolean isValid = true;
        String errorMessage = "";
        numeroTarjeta = numeroTarjeta.replaceAll("[^0-9]", "");
        if (!numeroTarjeta.matches("\\d{16}")) {
            isValid = false;
            errorMessage = "El número de tarjeta debe tener 16 dígitos.";
        } else if (!isValidLuhn(numeroTarjeta)) {
            isValid = false;
            errorMessage = "El número de tarjeta no es válido.";
        }
        if (nombreTitular == null || nombreTitular.trim().isEmpty()) {
            isValid = false;
            errorMessage = "El nombre del titular es requerido.";
        }
        if (!fechaExpiracion.matches("(0[1-9]|1[0-2])/\\d{2}")) {
            isValid = false;
            errorMessage = "La fecha de expiración debe estar en formato MM/AA.";
        } else {
            try {
                String[] parts = fechaExpiracion.split("/");
                int mes = Integer.parseInt(parts[0]);
                int anio = Integer.parseInt(parts[1]) + 2000;
                LocalDate expiryDate = LocalDate.of(anio, mes, 1);
                if (expiryDate.isBefore(LocalDate.now().withDayOfMonth(1))) {
                    isValid = false;
                    errorMessage = "La tarjeta está vencida.";
                }
            } catch (Exception e) {
                isValid = false;
                errorMessage = "Formato de fecha de expiración inválido.";
            }
        }
        if (!cvv.matches("\\d{3,4}")) {
            isValid = false;
            errorMessage = "El CVV debe tener 3 o 4 dígitos.";
        }

        // Calcular monto
        long duracionHoras = Duration.between(reserva.getInicioReserva(), reserva.getFinReserva()).toHours();
        BigDecimal monto = reserva.getEspacioDeportivo().getPrecioPorHora().multiply(BigDecimal.valueOf(duracionHoras));

        // Crear o actualizar pago
        Optional<Pago> optPago = pagoRepository.findByReserva(reserva);
        Pago pago = optPago.orElse(new Pago());
        pago.setReserva(reserva);
        pago.setMetodoPago(metodoPagoRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("Método de pago 'Pago Online' no encontrado")));
        pago.setMonto(monto);
        pago.setFechaPago(LocalDateTime.now());
        pago.setDetallesTransaccion("Pago simulado con tarjeta terminada en " + numeroTarjeta.substring(12));

        if (isValid) {
            pago.setEstadoTransaccion(Pago.EstadoTransaccion.completado);
            reserva.setEstado(Reserva.Estado.completada);
            reserva.setFechaActualizacion(LocalDateTime.now());
            redirectAttributes.addFlashAttribute("mensaje", "Pago realizado con éxito");
        } else {
            pago.setEstadoTransaccion(Pago.EstadoTransaccion.fallido);
            pago.setMotivoRechazo(errorMessage);
            redirectAttributes.addFlashAttribute("error", "Pago rechazado: " + errorMessage);
        }

        pagoRepository.save(pago);
        reservaRepository.save(reserva);
        return "redirect:/usuarios/mis-reservas";
    }

    // Mostrar formulario de pago por depósito
    @GetMapping("/pago-deposito/{reservaId}")
    public String mostrarPagoDeposito(@PathVariable("reservaId") Integer reservaId, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            redirectAttributes.addFlashAttribute("error", "Por favor, inicia sesión para pagar.");
            return "redirect:/usuarios/inicio";
        }

        Optional<Reserva> optReserva = reservaRepository.findById(reservaId);
        if (optReserva.isEmpty() || !optReserva.get().getUsuario().getUsuarioId().equals(usuario.getUsuarioId())) {
            redirectAttributes.addFlashAttribute("error", "Reserva no encontrada o no autorizada");
            return "redirect:/usuarios/mis-reservas";
        }

        Reserva reserva = optReserva.get();
        if (!reserva.getEstado().equals(Reserva.Estado.pendiente)) {
            redirectAttributes.addFlashAttribute("error", "La reserva no está pendiente de pago");
            return "redirect:/usuarios/mis-reservas";
        }

        model.addAttribute("reserva", reserva);
        model.addAttribute("activeItem", "reservas");
        return "vecino/vecino-pago-deposito";
    }

    // Procesar pago por depósito con subida de comprobante
    /*
    @PostMapping("/pago-deposito/{reservaId}")
    public String procesarPagoDeposito(@PathVariable("reservaId") Integer reservaId,
                                       @RequestParam("comprobante") MultipartFile comprobante,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            redirectAttributes.addFlashAttribute("error", "Por favor, inicia sesión para pagar.");
            return "redirect:/usuarios/inicio";
        }

        Optional<Reserva> optReserva = reservaRepository.findById(reservaId);
        if (optReserva.isEmpty() || !optReserva.get().getUsuario().getUsuarioId().equals(usuario.getUsuarioId())) {
            redirectAttributes.addFlashAttribute("error", "Reserva no encontrada o no autorizada");
            return "redirect:/usuarios/mis-reservas";
        }

        Reserva reserva = optReserva.get();
        if (!reserva.getEstado().equals(Reserva.Estado.pendiente)) {
            redirectAttributes.addFlashAttribute("error", "La reserva no está pendiente de pago");
            return "redirect:/usuarios/mis-reservas";
        }

        // Validar comprobante
        if (comprobante.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Debe subir un comprobante de pago.");
            return "redirect:/usuarios/pago-deposito/" + reservaId;
        }
        String contentType = comprobante.getContentType();
        if (contentType == null || !contentType.matches("^(image/(jpeg|png|jpg)|application/pdf)$")) {
            redirectAttributes.addFlashAttribute("error", "El comprobante debe ser una imagen (JPEG, PNG, JPG) o PDF.");
            return "redirect:/usuarios/pago-deposito/" + reservaId;
        }

        // Calcular monto
        long duracionHoras = Duration.between(reserva.getInicioReserva(), reserva.getFinReserva()).toHours();
        BigDecimal monto = reserva.getEspacioDeportivo().getPrecioPorHora().multiply(BigDecimal.valueOf(duracionHoras));

        // Subir comprobante a S3
        String comprobanteUrl;
        try {
            comprobanteUrl = s3Service.uploadFile(comprobante);
            if (!comprobanteUrl.contains("URL:")) {
                redirectAttributes.addFlashAttribute("error", "Error al subir el comprobante: " + comprobanteUrl);
                return "redirect:/usuarios/pago-deposito/" + reservaId;
            }
            comprobanteUrl = comprobanteUrl.substring(comprobanteUrl.indexOf("URL: ") + 5).trim();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al subir el comprobante: " + e.getMessage());
            return "redirect:/usuarios/pago-deposito/" + reservaId;
        }

        // Crear o actualizar pago
        Optional<Pago> optPago = pagoRepository.findByReserva(reserva);
        Pago pago = optPago.orElse(new Pago());
        pago.setReserva(reserva);
        pago.setMetodoPago(metodoPagoRepository.findById(2)
                .orElseThrow(() -> new RuntimeException("Método de pago 'Depósito' no encontrado")));
        pago.setMonto(monto);
        pago.setFechaPago(LocalDateTime.now());
        pago.setEstadoTransaccion(Pago.EstadoTransaccion.pendiente);
        pago.setDetallesTransaccion("Comprobante subido: " + comprobanteUrl);
        pagoRepository.save(pago);

        redirectAttributes.addFlashAttribute("mensaje", "Pago por depósito registrado. Pendiente de validación.");
        return "redirect:/usuarios/mis-reservas";
    }

     */
    @PostMapping("/pago-deposito/{reservaId}")
    public String procesarPagoDeposito(@PathVariable("reservaId") Integer reservaId,
                                       @RequestParam("comprobante") MultipartFile comprobante,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            redirectAttributes.addFlashAttribute("error", "Por favor, inicia sesión para pagar.");
            return "redirect:/usuarios/inicio";
        }

        Optional<Reserva> optReserva = reservaRepository.findById(reservaId);
        if (optReserva.isEmpty() || !optReserva.get().getUsuario().getUsuarioId().equals(usuario.getUsuarioId())) {
            redirectAttributes.addFlashAttribute("error", "Reserva no encontrada o no autorizada");
            return "redirect:/usuarios/mis-reservas";
        }

        Reserva reserva = optReserva.get();
        if (!reserva.getEstado().equals(Reserva.Estado.pendiente)) {
            redirectAttributes.addFlashAttribute("error", "La reserva no está pendiente de pago");
            return "redirect:/usuarios/mis-reservas";
        }

        // Validar comprobante
        if (comprobante.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Debe subir un comprobante de pago.");
            return "redirect:/usuarios/pago-deposito/" + reservaId;
        }
        String contentType = comprobante.getContentType();
        if (contentType == null || !contentType.matches("^(image/(jpeg|png|jpg)|application/pdf)$")) {
            redirectAttributes.addFlashAttribute("error", "El comprobante debe ser una imagen (JPEG, PNG, JPG) o PDF.");
            return "redirect:/usuarios/pago-deposito/" + reservaId;
        }

        // Calcular monto
        long duracionHoras = Duration.between(reserva.getInicioReserva(), reserva.getFinReserva()).toHours();
        BigDecimal monto = reserva.getEspacioDeportivo().getPrecioPorHora().multiply(BigDecimal.valueOf(duracionHoras));

        // Subir comprobante a S3 con manejo de errores
        String comprobanteUrl = null;
        try {
            String uploadResult = s3Service.uploadFile(comprobante);
            if (uploadResult != null && uploadResult.contains("URL:") && !uploadResult.trim().isEmpty()) {
                comprobanteUrl = uploadResult.substring(uploadResult.indexOf("URL: ") + 5).trim();
                // Validar longitud de la URL
                if (comprobanteUrl.length() > 255) {
                    redirectAttributes.addFlashAttribute("error", "La URL del comprobante excede los 255 caracteres.");
                    return "redirect:/usuarios/pago-deposito/" + reservaId;
                }
                if (comprobanteUrl.isEmpty()) {
                    redirectAttributes.addFlashAttribute("error", "La URL del comprobante está vacía.");
                    return "redirect:/usuarios/pago-deposito/" + reservaId;
                }
            } else {
                redirectAttributes.addFlashAttribute("error", "Error al subir el comprobante: " + (uploadResult != null ? uploadResult : "Resultado inválido"));
                return "redirect:/usuarios/pago-deposito/" + reservaId;
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al subir el comprobante: " + e.getMessage());
            return "redirect:/usuarios/pago-deposito/" + reservaId;
        }

        // Crear o actualizar pago
        Optional<Pago> optPago = pagoRepository.findByReserva(reserva);
        Pago pago = optPago.orElse(new Pago());
        pago.setReserva(reserva);
        pago.setMetodoPago(metodoPagoRepository.findById(2)
                .orElseThrow(() -> new RuntimeException("Método de pago 'Depósito' no encontrado")));
        pago.setMonto(monto);
        pago.setFechaPago(LocalDateTime.now());
        pago.setEstadoTransaccion(Pago.EstadoTransaccion.pendiente);
        pago.setFotoComprobanteUrl(comprobanteUrl); // Store URL in foto_comprobante_url
        pago.setDetallesTransaccion("Comprobante subido: Pendiente revisión del Administrador"); // Update detalles_transaccion
        pagoRepository.save(pago);

        redirectAttributes.addFlashAttribute("mensaje", "Pago por depósito registrado. Pendiente de validación.");
        return "redirect:/usuarios/mis-reservas";
    }

    // Algoritmo de Luhn para validar número de tarjeta
    private boolean isValidLuhn(String numeroTarjeta) {
        int sum = 0;
        boolean alternate = false;
        for (int i = numeroTarjeta.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(numeroTarjeta.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }    @GetMapping("/api/reservas/espacio/{espacioId}")
    @ResponseBody
    public List<Map<String, Object>> obtenerReservasPorEspacio(
            @PathVariable("espacioId") Integer espacioId,
            HttpSession session
    ) {
        // Obtener el usuario actual de la sesión
        Usuario usuarioActual = (Usuario) session.getAttribute("usuario");
        Integer usuarioId = usuarioActual != null ? usuarioActual.getUsuarioId() : null;
        
        // Obtener todas las reservas para el espacio deportivo
        List<Reserva> reservas = reservaRepository.findByEspacioDeportivo_EspacioDeportivoId(espacioId);
        
        // Lista para almacenar tanto eventos de reserva como eventos fantasma
        List<Map<String, Object>> eventos = new ArrayList<>();
        
        // Obtener la hora actual para determinar qué eventos están en el pasado
        LocalDateTime ahora = LocalDateTime.now();
        
        // Transformar las reservas a eventos
        for (Reserva r : reservas) {
            Map<String, Object> eventoReserva = new HashMap<>();
            eventoReserva.put("id", r.getReservaId().toString());
            eventoReserva.put("start", r.getInicioReserva().toString());
            eventoReserva.put("end", r.getFinReserva().toString());
            
            // Comprobar si la reserva pertenece al usuario actual
            boolean esReservaPropia = usuarioId != null && r.getUsuario().getUsuarioId().equals(usuarioId);
            
            // Comprobar si la reserva está en el pasado
            boolean esPasada = r.getFinReserva().isBefore(ahora);
            
            // Configurar propiedades según si es propia y si es pasada
            if (esReservaPropia) {
                if (esPasada) {
                    eventoReserva.put("title", "Mi reserva (Pasada)");
                    eventoReserva.put("className", "evento-propio-pasado");
                    eventoReserva.put("backgroundColor", "#8f9bdb"); // Azul más claro/grisáceo
                    eventoReserva.put("borderColor", "#8f9bdb");
                    eventoReserva.put("textColor", "#555555");
                } else {
                    eventoReserva.put("title", "Mi reserva");
                    eventoReserva.put("className", "evento-propio");
                    eventoReserva.put("backgroundColor", "#5664d2"); // Azul
                    eventoReserva.put("borderColor", "#5664d2");
                    eventoReserva.put("textColor", "#ffffff");
                }
            } else {
                if (esPasada) {
                    eventoReserva.put("title", "Reservado (Pasado)");
                    eventoReserva.put("className", "evento-ajeno-pasado");
                    eventoReserva.put("backgroundColor", "#808080"); // Gris más oscuro
                    eventoReserva.put("borderColor", "#707070");
                    eventoReserva.put("textColor", "#444444");
                } else {
                    eventoReserva.put("title", "Reservado");
                    eventoReserva.put("className", "evento-ajeno");
                    eventoReserva.put("backgroundColor", "#a0a0a0"); // Gris
                    eventoReserva.put("borderColor", "#909090");
                    eventoReserva.put("textColor", "#ffffff");
                }
            }
              eventoReserva.put("esPropia", esReservaPropia);
            eventoReserva.put("esPasada", esPasada);
            eventoReserva.put("estado", r.getEstado().name().toLowerCase());
            eventoReserva.put("editable", false); // Ninguna reserva es editable directamente
            
            eventos.add(eventoReserva);
        }
        
        // Obtener el espacio deportivo para saber horarios de operación
        Optional<EspacioDeportivo> optEspacio = espacioDeportivoRepository.findById(espacioId);
        if (optEspacio.isPresent()) {
            EspacioDeportivo espacio = optEspacio.get();
            
            // Aquí deberíamos obtener horarios de operación del establecimiento
            // Por ahora asumimos 8:00 AM a 10:00 PM como horario general
            LocalTime horaInicio = LocalTime.of(8, 0);
            LocalTime horaFin = LocalTime.of(22, 0);
            
            // Definir el rango de fechas para generar eventos fantasma
            // Desde hace 7 días hasta la hora actual
            LocalDateTime inicioRango = ahora.minusDays(7)
                .withHour(horaInicio.getHour())
                .withMinute(horaInicio.getMinute())
                .withSecond(0)
                .withNano(0);
                
            // Generar eventos fantasma para cada slot de tiempo pasado sin reserva
            // Recorremos día por día
            for (LocalDateTime diaActual = inicioRango.toLocalDate().atStartOfDay(); 
                 !diaActual.toLocalDate().isAfter(ahora.toLocalDate()); 
                 diaActual = diaActual.plusDays(1)) {
                
                // Para cada día, recorremos los slots de tiempo dentro del horario de operación
                LocalDateTime inicioSlot = diaActual.with(horaInicio);
                LocalDateTime finDia = diaActual.with(horaFin);
                
                // Si el día es hoy, solo generamos eventos hasta la hora actual
                LocalDateTime limiteFinal = diaActual.toLocalDate().equals(ahora.toLocalDate()) 
                    ? ahora : finDia;
                    
                // Generamos slots de 1 hora
                while (inicioSlot.isBefore(limiteFinal)) {
                    LocalDateTime finSlot = inicioSlot.plusHours(1);
                    
                    // Solo procesamos slots que ya pasaron
                    if (finSlot.isBefore(ahora)) {
                        // Verificar que este slot no tiene una reserva existente
                        final LocalDateTime inicioSlotFinal = inicioSlot;
                        final LocalDateTime finSlotFinal = finSlot;
                        
                        boolean existeReserva = reservas.stream().anyMatch(r -> {
                            // Verificamos si hay solapamiento entre el slot y la reserva
                            return !(finSlotFinal.isBefore(r.getInicioReserva()) || 
                                   inicioSlotFinal.isAfter(r.getFinReserva()));
                        });
                        
                        // Si no hay reserva para este slot pasado, creamos un evento fantasma
                        if (!existeReserva) {
                            Map<String, Object> eventoFantasma = new HashMap<>();
                            
                            String slotId = "fantasma-" + inicioSlot.toString();
                            eventoFantasma.put("id", slotId);
                            eventoFantasma.put("title", "No disponible");
                            eventoFantasma.put("start", inicioSlot.toString());
                            eventoFantasma.put("end", finSlot.toString());
                            eventoFantasma.put("className", "evento-fantasma");
                            eventoFantasma.put("backgroundColor", "#d0d0d0"); // Gris claro
                            eventoFantasma.put("borderColor", "#c0c0c0");
                            eventoFantasma.put("textColor", "#707070");
                            eventoFantasma.put("editable", false);
                            eventoFantasma.put("esPasado", true);
                            eventoFantasma.put("esFantasma", true); // Para identificarlo en el cliente
                            
                            eventos.add(eventoFantasma);
                        }
                    }
                    
                    // Avanzamos al siguiente slot
                    inicioSlot = finSlot;
                }
            }
        }
        
        return eventos;
    }

    @GetMapping("/api/reservas/{reservaId}")
    @ResponseBody
    public ResponseEntity<?> obtenerDetallesReserva(
            @PathVariable("reservaId") Integer reservaId,
            HttpSession session
    ) {
        // Obtener el usuario actual de la sesión
        Usuario usuarioActual = (Usuario) session.getAttribute("usuario");
        if (usuarioActual == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }
        
        // Buscar la reserva por ID
        Optional<Reserva> optReserva = reservaRepository.findById(reservaId);
        if (optReserva.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Reserva no encontrada");
        }
        
        Reserva reserva = optReserva.get();
        
        // Verificar que la reserva pertenezca al usuario actual
        if (!reserva.getUsuario().getUsuarioId().equals(usuarioActual.getUsuarioId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No tiene permisos para ver esta reserva");
        }
        
        // Crear un mapa con los detalles de la reserva
        Map<String, Object> detallesReserva = new HashMap<>();
        detallesReserva.put("reservaId", reserva.getReservaId());
        detallesReserva.put("inicioReserva", reserva.getInicioReserva().toString());
        detallesReserva.put("finReserva", reserva.getFinReserva().toString());
        detallesReserva.put("estado", reserva.getEstado().name());
        detallesReserva.put("fechaCreacion", reserva.getFechaCreacion().toString());
        
        // Calcular duración en horas
        long duracionHoras = Duration.between(reserva.getInicioReserva(), reserva.getFinReserva()).toHours();
        detallesReserva.put("duracionHoras", duracionHoras);
        
        // Calcular precio total
        BigDecimal precioTotal = reserva.getEspacioDeportivo().getPrecioPorHora().multiply(BigDecimal.valueOf(duracionHoras));
        detallesReserva.put("precioTotal", precioTotal);
        
        // Información del espacio deportivo
        detallesReserva.put("espacioDeportivo", reserva.getEspacioDeportivo().getNombre());
        detallesReserva.put("tipoServicio", reserva.getEspacioDeportivo().getServicioDeportivo().getServicioDeportivo());
        detallesReserva.put("establecimiento", reserva.getEspacioDeportivo().getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre());
        
        return ResponseEntity.ok(detallesReserva);
    }

}
