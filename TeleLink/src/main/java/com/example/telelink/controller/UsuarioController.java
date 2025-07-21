package com.example.telelink.controller;

import com.example.telelink.service.NotificacionService;
import com.example.telelink.dto.vecino.PagoRequest;
import com.example.telelink.entity.*;
import com.example.telelink.repository.*;
import com.example.telelink.service.EmailService;
import com.example.telelink.service.S3Service;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import java.time.format.DateTimeParseException;
import java.util.*;
import java.io.*;
import java.net.*;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import com.example.telelink.dto.ReservaCalendarioDTO;

import org.springframework.format.annotation.DateTimeFormat;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private NotificacionService notificacionService;

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

    @Autowired
    private ReembolsoRepository reembolsoRepository;

    @Autowired
    private ReseniaRepository reseniaRepository;

    @Autowired
    private EmailService emailService;

    public class EspacioDeportivoConRatingDTO {
        private EspacioDeportivo espacioDeportivo;
        private double promedioCalificacion;
        private long reviewCount;

        public EspacioDeportivoConRatingDTO(EspacioDeportivo espacioDeportivo, double promedioCalificacion,
                long reviewCount) {
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
                        ((Number) result[2]).longValue()))
                .collect(Collectors.toList());

        // Pasar datos al modelo
        model.addAttribute("usuario", usuario);
        model.addAttribute("activeItem", "inicio");
        model.addAttribute("ultimoAviso", ultimoAviso);
        model.addAttribute("canchasPopulares", canchasPopulares);
        return "Vecino/vecino-index";
    }

    @GetMapping("/reservas/{id}")
    public String mostrarReservation(@PathVariable Integer id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model, HttpSession session) {

        // Verificar usuario logueado
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/usuarios/inicio";
        }

        // Obtener el espacio deportivo
        EspacioDeportivo espacio = espacioDeportivoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el espacio deportivo"));

        // Obtener TODAS las reseñas (sin paginación) para el frontend
        List<Resenia> todasLasResenias = reseniaRepository
                .findByEspacioDeportivo_EspacioDeportivoIdOrderByFechaCreacionDesc(id);

        // Calcular el promedio
        double promedio = todasLasResenias.stream()
                .mapToInt(Resenia::getCalificacion)
                .average()
                .orElse(0.0);

        // Pasar todos los datos al modelo
        model.addAttribute("espacio", espacio);
        model.addAttribute("resenias", todasLasResenias);
        model.addAttribute("totalResenias", todasLasResenias.size());
        model.addAttribute("promedioCalificacion", promedio);
        model.addAttribute("usuario", usuario);

        return "Vecino/vecino-servicioDeportivo";
    }

    @GetMapping("/reembolsos")
    public String mostrarMisReembolsos(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/usuarios/inicio";
        }

        List<Reembolso> reembolsos = reembolsoRepository.findByPago_Reserva_Usuario_UsuarioId(usuario.getUsuarioId());

        model.addAttribute("usuario", usuario);
        model.addAttribute("reembolsos", reembolsos);
        model.addAttribute("activeItem", "reembolsos");

        return "Vecino/vecino-mis-reembolsos";
    }

    @GetMapping("/resenia")
    public String mostrarMisResenias(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null)
            return "redirect:/usuarios/inicio";

        // Crear objeto Pageable para la paginación
        Pageable pageable = PageRequest.of(page, size, Sort.by("fechaCreacion").descending());

        // Obtener las reseñas paginadas
        Page<Resenia> reseniasPaginadas = reseniaRepository.findByUsuario_UsuarioId(usuario.getUsuarioId(), pageable);

        // Calcular información de paginación
        int totalPaginas = reseniasPaginadas.getTotalPages();
        long totalElementos = reseniasPaginadas.getTotalElements();
        int paginaActual = page + 1; // +1 porque page es 0-based
        int elementoInicio = (page * size) + 1;
        int elementoFin = Math.min(elementoInicio + size - 1, (int) totalElementos);

        // Generar lista de números de página para la navegación
        List<Integer> numerosPagina = new ArrayList<>();
        int inicio = Math.max(1, paginaActual - 2);
        int fin = Math.min(totalPaginas, paginaActual + 2);

        for (int i = inicio; i <= fin; i++) {
            numerosPagina.add(i);
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("resenias", reseniasPaginadas.getContent());
        model.addAttribute("totalResenias", totalElementos);
        model.addAttribute("activeItem", "resenia");

        // Atributos para paginación
        model.addAttribute("paginaActual", paginaActual);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("elementoInicio", elementoInicio);
        model.addAttribute("elementoFin", elementoFin);
        model.addAttribute("totalElementos", totalElementos);
        model.addAttribute("numerosPagina", numerosPagina);
        model.addAttribute("tienePaginaAnterior", reseniasPaginadas.hasPrevious());
        model.addAttribute("tienePaginaSiguiente", reseniasPaginadas.hasNext());
        model.addAttribute("paginaAnterior", Math.max(0, page - 1));
        model.addAttribute("paginaSiguiente", Math.min(totalPaginas - 1, page + 1));
        model.addAttribute("tamanioPagina", size);

        return "Vecino/vecino-mis-resenias";
    }

    @PostMapping("/agregar-resenia/{espacioId}")
    public String agregarResenia(
            @PathVariable("espacioId") Integer espacioId,
            @RequestParam("calificacion") Integer calificacion,
            @RequestParam("comentario") String comentario,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            redirectAttributes.addFlashAttribute("error", "Debes iniciar sesión para agregar una reseña.");
            return "redirect:/usuarios/inicio";
        }

        // Verificar si el espacio deportivo existe
        Optional<EspacioDeportivo> optEspacio = espacioDeportivoRepository.findById(espacioId);
        if (optEspacio.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Espacio deportivo no encontrado.");
            return "redirect:/usuarios/cancha";
        }

        // Crear y guardar la reseña
        Resenia resenia = new Resenia();
        resenia.setUsuario(usuario);
        resenia.setEspacioDeportivo(optEspacio.get());
        resenia.setCalificacion(calificacion);
        resenia.setComentario(comentario);
        resenia.setFechaCreacion(LocalDateTime.now());

        reseniaRepository.save(resenia);

        redirectAttributes.addFlashAttribute("success", "Reseña agregada exitosamente.");
        return "redirect:/usuarios/reservas/" + espacioId;
    }

    @GetMapping("/perfil")
    public String mostrarPerfil(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        model.addAttribute("usuario", usuario);
        return "Vecino/vecino-perfil";
    }

    @GetMapping("/editar-perfil")
    public String mostrarEditarPerfil(@ModelAttribute("usuario") Usuario usuarioActualizado, Model model,
            HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        model.addAttribute("usuario", usuario);
        return "Vecino/vecino-editarPerfil";
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

        // Validar el formato de la foto si se subió una y asociar el error a
        // fotoPerfilUrl
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
            return "Vecino/vecino-editarPerfil";
        }

        // Actualizar los campos permitidos (teléfono)
        usuario.setTelefono(usuarioActualizado.getTelefono());

        // Manejar la subida de la foto al bucket S3
        String defaultFotoPerfilUrl = usuario.getFotoPerfilUrl() != null ? usuario.getFotoPerfilUrl()
                : "https://img.freepik.com/foto-gratis/disparo-cabeza-hombre-atractivo-sonriendo-complacido-mirando-intrigado-pie-sobre-fondo-azul_1258-65733.jpg";
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
                redirectAttributes.addFlashAttribute("message",
                        "Error al subir la foto de perfil: " + uploadResult + ". Se usó una imagen por defecto.");
                redirectAttributes.addFlashAttribute("messageType", "error");
            } else {
                // Caso inesperado
                System.out.println("Resultado inválido de la subida: " + uploadResult);
                usuario.setFotoPerfilUrl(defaultFotoPerfilUrl);
                redirectAttributes.addFlashAttribute("message",
                        "Error desconocido al subir la foto. Se usó una imagen por defecto.");
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

        return "Vecino/vecino-pago";
    }

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
            BigDecimal precioTotal = reserva.getEspacioDeportivo().getPrecioPorHora()
                    .multiply(BigDecimal.valueOf(duracionHoras));
            reservaMap.put("precioTotal", precioTotal);

            // Check if payment exists and is completed
            Optional<Pago> pagoOpt = pagoRepository.findByReserva(reserva);

            // Lógica más flexible para determinar si el pago está completado
            boolean isPagado = false;
            boolean pagoCompletado = false;

            if (pagoOpt.isPresent()) {
                String estadoTransaccion = pagoOpt.get().getEstadoTransaccion().name().toLowerCase();

                // Usar la misma lógica que en mostrarComprobantePago
                pagoCompletado = estadoTransaccion.equals("completado") ||
                        estadoTransaccion.equals("completed") ||
                        estadoTransaccion.equals("exitoso") ||
                        estadoTransaccion.equals("success");

                isPagado = pagoCompletado;
            }

            reservaMap.put("isPagado", isPagado);

            // Add payment object and completion status explicitly
            if (pagoOpt.isPresent()) {
                reservaMap.put("pago", pagoOpt.get());
                reservaMap.put("pagoCompletado", pagoCompletado);
            } else {
                reservaMap.put("pago", null);
                reservaMap.put("pagoCompletado", false);
            }

            return reservaMap;
        }).collect(Collectors.toList());

        model.addAttribute("usuario", usuario);
        model.addAttribute("reservasWithCancelFlag", reservasWithCancelFlag);
        model.addAttribute("activeItem", "reservas");
        return "Vecino/vecino-mis-reservas";
    }

    @GetMapping("/comprobante-pago/{reservaId}")
    public String mostrarComprobantePago(@PathVariable("reservaId") Integer reservaId,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            redirectAttributes.addFlashAttribute("error", "Por favor, inicia sesión para ver el comprobante.");
            return "redirect:/usuarios/inicio";
        }

        Optional<Reserva> optReserva = reservaRepository.findById(reservaId);
        if (optReserva.isEmpty() || !optReserva.get().getUsuario().getUsuarioId().equals(usuario.getUsuarioId())) {
            redirectAttributes.addFlashAttribute("error", "Reserva no encontrada o no autorizada");
            return "redirect:/usuarios/mis-reservas";
        }

        Reserva reserva = optReserva.get();
        Optional<Pago> optPago = pagoRepository.findByReserva(reserva);

        if (optPago.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No se encontró un pago asociado a esta reserva");
            return "redirect:/usuarios/mis-reservas";
        }

        Pago pago = optPago.get();

        // Verificar si el pago está completado (con lógica flexible)
        String estadoTransaccion = pago.getEstadoTransaccion().name().toLowerCase();
        boolean pagoCompletado = estadoTransaccion.equals("completado") ||
                estadoTransaccion.equals("completed") ||
                estadoTransaccion.equals("exitoso") ||
                estadoTransaccion.equals("success");

        if (!pagoCompletado) {
            redirectAttributes.addFlashAttribute("error", "El pago no ha sido completado");
            return "redirect:/usuarios/mis-reservas";
        }

        // Calcular duración y precio total
        long duracionHoras = Duration.between(reserva.getInicioReserva(), reserva.getFinReserva()).toHours();
        BigDecimal precioTotal = reserva.getEspacioDeportivo().getPrecioPorHora()
                .multiply(BigDecimal.valueOf(duracionHoras));

        // Si es piscina y tiene número de participantes, multiplicar por ese número
        if ("piscina".equalsIgnoreCase(reserva.getEspacioDeportivo().getServicioDeportivo().getServicioDeportivo())
                && reserva.numeroParticipantes() != null
                && reserva.numeroParticipantes() > 0) {
            precioTotal = precioTotal.multiply(BigDecimal.valueOf(reserva.numeroParticipantes()));
        }

        model.addAttribute("reserva", reserva);
        model.addAttribute("pago", pago);
        model.addAttribute("duracionHoras", duracionHoras);
        model.addAttribute("precioTotal", precioTotal);

        return "Vecino/vecino-comprobantePago";
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
        boolean dentroDe48Horas = ahora.isAfter(limite);

        // Cambiar el estado de la reserva a cancelada
        reserva.setEstado(Reserva.Estado.cancelada);
        reserva.setRazonCancelacion(razon != null ? razon : "Cancelación por el usuario");
        reserva.setFechaActualizacion(LocalDateTime.now());
        reservaRepository.save(reserva);

        // Buscar el pago asociado a la reserva
        Optional<Pago> pagoOptional = pagoRepository.findByReserva(reserva);
        String mensaje;

        if (pagoOptional.isPresent()) {
            Pago pago = pagoOptional.get();

            if (!dentroDe48Horas) {
                // Elegible para reembolso (cancelación con 48+ horas de anticipación)
                Reembolso reembolso = new Reembolso();
                reembolso.setMonto(pago.getMonto());
                reembolso.setMotivo(razon != null ? razon : "Cancelación de reserva");
                reembolso.setFechaReembolso(LocalDateTime.now());
                reembolso.setPago(pago);

                // Verificar el método de pago
                if (pago.getMetodoPago().getMetodoPago().equals("Pago Online")) {
                    // Reembolso instantáneo para Pago Online
                    reembolso.setEstado(Reembolso.Estado.completado);
                    reembolso.setDetallesTransaccion("Reembolso procesado automáticamente para Pago Online");
                    mensaje = "Reserva cancelada y reembolso procesado automáticamente.";
                } else {
                    // Depósito Bancario: requiere aprobación del administrador
                    reembolso.setEstado(Reembolso.Estado.pendiente);
                    reembolso.setDetallesTransaccion("Esperando aprobación del administrador");
                    mensaje = "Reserva cancelada. El reembolso está pendiente de aprobación.";
                }

                reembolsoRepository.save(reembolso);
            } else {
                // No elegible para reembolso
                mensaje = "Reserva cancelada, pero no se procesó reembolso debido a cancelación con menos de 48 horas.";
            }
        } else {
            // No hay pago asociado
            mensaje = "Reserva cancelada correctamente. No se encontró un pago asociado.";
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
        return "Vecino/vecino-cancha";
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
                    .filter(espacio -> espacio.getNombre().toLowerCase().contains(busquedaLower) ||
                            (espacio.getDescripcion() != null
                                    && espacio.getDescripcion().toLowerCase().contains(busquedaLower))
                            ||
                            espacio.getServicioDeportivo().getServicioDeportivo().toLowerCase().contains(busquedaLower)
                            ||
                            espacio.getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre().toLowerCase()
                                    .contains(busquedaLower))
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

        return "Vecino/vecino-ayuda";
    }

    @GetMapping("/calendario")
    public String mostrarCalendario(Model model) {
        // Aquí puedes agregar cualquier lógica que necesites
        return "Vecino/vecino-calendario";
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

        // Obtener el espacio deportivo
        EspacioDeportivo espacio = espacioDeportivoRepository.findById(espacioId)
                .orElseThrow(() -> new RuntimeException("Espacio no encontrado"));

        List<Reserva> reservasUsuario = reservaRepository.findByUsuario_UsuarioId(usuario.getUsuarioId());

        // Siempre agregar datos básicos al modelo
        model.addAttribute("usuario", usuario);
        model.addAttribute("reservas", reservasUsuario);
        model.addAttribute("espacio", espacio);
        model.addAttribute("espacioSeleccionadoId", espacioId);

        // Si hay parámetros de tiempo, procesar la reserva
        if (inicio != null && fin != null) {
            try {
                OffsetDateTime odtInicio = OffsetDateTime.parse(inicio);
                OffsetDateTime odtFin = OffsetDateTime.parse(fin);

                ZoneId zonaLocal = ZoneId.of("America/Lima");
                LocalDateTime inicioLDT = odtInicio.atZoneSameInstant(zonaLocal).toLocalDateTime();
                LocalDateTime finLDT = odtFin.atZoneSameInstant(zonaLocal).toLocalDateTime();

                // Verificar si ya hay una reserva que se superponga usando el nuevo método
                long conflictos = reservaRepository.countActiveReservationConflicts(espacioId, inicioLDT, finLDT);

                if (conflictos > 0) {
                    model.addAttribute("error", "Ya existe una reserva en este horario.");
                    return "Vecino/vecino-reservar";
                }

                // Crear reserva en estado pendiente
                Reserva reserva = new Reserva();
                reserva.setUsuario(usuario);
                reserva.setEspacioDeportivo(espacio);
                reserva.setInicioReserva(inicioLDT);
                reserva.setFinReserva(finLDT);
                reserva.setEstado(Reserva.Estado.pendiente);
                reserva.setFechaCreacion(LocalDateTime.now());

                // Calcular duración y precio
                long duracionHoras = Duration.between(inicioLDT, finLDT).toHours();
                if (duracionHoras == 0) {
                    duracionHoras = 1; // Mínimo 1 hora
                }

                BigDecimal precioPorHora = espacio.getPrecioPorHora();
                BigDecimal precioTotal = precioPorHora.multiply(BigDecimal.valueOf(duracionHoras));

                // Formatear fechas para mostrar
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
                String fechaFormateada = inicioLDT.format(formatter);
                String inicioFormateado = inicioLDT.format(timeFormatter);
                String finFormateado = finLDT.format(timeFormatter);

                // Guardar la reserva
                reservaRepository.save(reserva);

                // Agregar datos de la reserva al modelo para mostrar en el template
                model.addAttribute("reserva", reserva);
                model.addAttribute("fechaReserva", fechaFormateada);
                model.addAttribute("inicioReserva", inicioFormateado);
                model.addAttribute("finReserva", finFormateado);
                model.addAttribute("duracionHoras", duracionHoras);
                model.addAttribute("precioTotal", precioTotal);
                model.addAttribute("tieneReserva", true);
                model.addAttribute("mensaje",
                        "Horario seleccionado correctamente. Complete los datos para confirmar su reserva.");

            } catch (DateTimeParseException e) {
                model.addAttribute("error", "Formato de fecha/hora inválido: " + e.getMessage());
            } catch (Exception e) {
                model.addAttribute("error", "Error al procesar la reserva: " + e.getMessage());
            }
        }

        return "Vecino/vecino-reservar";
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

        // Extrae el link de pago de la respuesta JSON (usa una librería JSON en
        // producción)
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        if (fecha == null)
            fecha = LocalDate.now();
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = inicio.plusDays(1);

        return reservaRepository.findByInicioReservaBetween(inicio, fin).stream()
                .map(r -> new ReservaCalendarioDTO(
                        r.getEspacioDeportivo().getEspacioDeportivoId().toString(),
                        getTitleWithParticipants(r),
                        r.getInicioReserva().toString(),
                        r.getFinReserva().toString(),
                        r.getNumeroCarrilPiscina(),
                        r.numeroParticipantes(),
                        r.getEspacioDeportivo().getServicioDeportivo().getServicioDeportivo()))
                .collect(Collectors.toList());
    }

    // Helper method to create a title that includes participant information for
    // pool reservations
    private String getTitleWithParticipants(Reserva reserva) {
        String baseTitle = "Reservado";
        if ("piscina".equalsIgnoreCase(reserva.getEspacioDeportivo().getServicioDeportivo().getServicioDeportivo())
                && reserva.numeroParticipantes() != null && reserva.numeroParticipantes() > 0) {
            return baseTitle + " (" + reserva.numeroParticipantes() + " participante" +
                    (reserva.numeroParticipantes() > 1 ? "s" : "") + ")";
        }
        return baseTitle;
    }

    // Modificado: Si no se encuentra el espacio, redirige a /usuarios/cancha con
    // mensaje de error
    @GetMapping("/reservasCalendario/{id}")
    public String verCalendarioReservas(@PathVariable("id") Integer id, Model model,
            RedirectAttributes redirectAttributes) {
        Optional<EspacioDeportivo> optEspacio = espacioDeportivoRepository.findById(id);
        if (optEspacio.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Espacio deportivo no encontrado");
            return "redirect:/usuarios/cancha";
        }

        EspacioDeportivo espacioDeportivo = optEspacio.get();
        model.addAttribute("espacioId", id);
        model.addAttribute("espacio", espacioDeportivo);

        // Determinar qué vista mostrar según el tipo de servicio deportivo
        String tipoServicio = espacioDeportivo.getServicioDeportivo().getServicioDeportivo().toLowerCase();

        if ("piscina".equals(tipoServicio)) {
            return "Vecino/reservas-piscina-calendario";
        } else if ("gimnasio".equals(tipoServicio)) {
            return "Vecino/reservas-gimnasio-calendario";
        } else if ("atletismo".equals(tipoServicio)) {
            return "Vecino/reservas-atletismo-calendario";
        } else {
            return "Vecino/reservas-futbol-calendario";
        }
    }
    // ---- Flujo de pagos ----

    @GetMapping("/pagar/{reservaId}")
    public String mostrarPagoReserva(@PathVariable("reservaId") Integer reservaId, Model model, HttpSession session,
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

        Optional<Pago> optPago = pagoRepository.findByReserva(reserva);
        if (optPago.isPresent() && optPago.get().getEstadoTransaccion().equals(Pago.EstadoTransaccion.completado)) {
            redirectAttributes.addFlashAttribute("error", "La reserva ya ha sido pagada");
            return "redirect:/usuarios/mis-reservas";
        }

        // Calcular duración y precio base
        long duracionHoras = Duration.between(reserva.getInicioReserva(), reserva.getFinReserva()).toHours();
        BigDecimal precioBase = reserva.getEspacioDeportivo().getPrecioPorHora()
                .multiply(BigDecimal.valueOf(duracionHoras));

        // Calcular precio total según tipo de servicio
        BigDecimal precioTotal = precioBase;

        // Si es piscina y tiene número de participantes, multiplicar por ese número
        if ("piscina".equalsIgnoreCase(reserva.getEspacioDeportivo().getServicioDeportivo().getServicioDeportivo())
                && reserva.numeroParticipantes() != null
                && reserva.numeroParticipantes() > 0) {
            precioTotal = precioBase.multiply(BigDecimal.valueOf(reserva.numeroParticipantes()));
        }

        model.addAttribute("reserva", reserva);
        model.addAttribute("duracionHoras", duracionHoras);
        model.addAttribute("precioTotal", precioTotal);
        model.addAttribute("activeItem", "reservas");
        return "Vecino/vecino-pagar";
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
    public String mostrarPagoTarjeta(@PathVariable("reservaId") Integer reservaId, Model model, HttpSession session,
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

        model.addAttribute("reserva", reserva);
        model.addAttribute("activeItem", "reservas");
        return "Vecino/vecino-pago-tarjeta";
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
        if (!reserva.getEstado().equals(Reserva.Estado.pendiente)) {
            redirectAttributes.addFlashAttribute("error", "La reserva no está pendiente de pago");
            return "redirect:/usuarios/mis-reservas";
        }

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
        } // Calcular monto
        long duracionHoras = Duration.between(reserva.getInicioReserva(), reserva.getFinReserva()).toHours();
        BigDecimal monto = reserva.getEspacioDeportivo().getPrecioPorHora().multiply(BigDecimal.valueOf(duracionHoras));

        // Si es una reserva de piscina, multiplicar por el número de participantes
        if ("piscina".equalsIgnoreCase(reserva.getEspacioDeportivo().getServicioDeportivo().getServicioDeportivo())
                && reserva.numeroParticipantes() != null
                && reserva.numeroParticipantes() > 0) {
            monto = monto.multiply(BigDecimal.valueOf(reserva.numeroParticipantes()));
        }

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
            reserva.setEstado(Reserva.Estado.confirmada);
            reserva.setFechaActualizacion(LocalDateTime.now());
            redirectAttributes.addFlashAttribute("mensaje", "Pago realizado con éxito");
            // Send email to user
            try {
                emailService.sendReservationPaymentCompleted(usuario, reserva, monto);
                System.out.println(
                        "✅ Correo de pago confirmado enviado exitosamente a: " + usuario.getCorreoElectronico());
            } catch (Exception emailException) {
                System.err.println("❌ Error al enviar correo de pago confirmado: " + emailException.getMessage());
            }
            notificacionService.crearNotificacion(
                    usuario.getUsuarioId(),
                    "creación",
                    "Reserva confirmada",
                    "Tu reserva en " + reserva.getEspacioDeportivo().getNombre() + " ha sido confirmada y pagada.",
                    "/usuarios/reservas/" + reserva.getEspacioDeportivo().getEspacioDeportivoId());
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
    public String mostrarPagoDeposito(@PathVariable("reservaId") Integer reservaId, Model model, HttpSession session,
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

        model.addAttribute("reserva", reserva);
        model.addAttribute("activeItem", "reservas");
        return "Vecino/vecino-pago-deposito";
    }

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
        } // Calcular monto
        long duracionHoras = Duration.between(reserva.getInicioReserva(), reserva.getFinReserva()).toHours();
        BigDecimal monto = reserva.getEspacioDeportivo().getPrecioPorHora().multiply(BigDecimal.valueOf(duracionHoras));

        // Si es una reserva de piscina, multiplicar por el número de participantes
        String tipoServicio = reserva.getEspacioDeportivo().getServicioDeportivo().getServicioDeportivo();
        if (("piscina".equalsIgnoreCase(tipoServicio)
                || "atletismo".equalsIgnoreCase(tipoServicio)
                || "gimnasio".equalsIgnoreCase(tipoServicio))
                && reserva.numeroParticipantes() != null
                && reserva.numeroParticipantes() > 0) {
            monto = monto.multiply(BigDecimal.valueOf(reserva.numeroParticipantes()));
        }

        // Subir comprobante a S3 with error handling
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
                redirectAttributes.addFlashAttribute("error", "Error al subir el comprobante: "
                        + (uploadResult != null ? uploadResult : "Resultado inválido"));
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
        pago.setDetallesTransaccion("Comprobante subido: Pendiente revisión del Administrador"); // Update
                                                                                                 // detalles_transaccion
        pagoRepository.save(pago);
        // Cambiar estado de la reserva a en_proceso
        reserva.setEstado(Reserva.Estado.en_proceso);
        reserva.setFechaActualizacion(LocalDateTime.now());
        reservaRepository.save(reserva);

        // Crear notificación al usuario
        notificacionService.crearNotificacion(
                usuario.getUsuarioId(),
                "creación",
                "Pago por depósito registrado",
                "Tu pago por depósito para la reserva en " + reserva.getEspacioDeportivo().getNombre()
                        + " ha sido registrado. Pendiente de validación.",
                "/usuarios/reservas/" + reserva.getEspacioDeportivo().getEspacioDeportivoId());

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
    }

    // ---- Fin de flujo de pagos ----

    @GetMapping("/api/reservas/espacio/{espacioId}")
    @ResponseBody
    public List<Map<String, Object>> obtenerReservasPorEspacio(
            @PathVariable("espacioId") Integer espacioId,
            HttpSession session) {
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
            eventoReserva.put("end", r.getFinReserva().toString()); // Comprobar si la reserva pertenece al usuario
                                                                    // actual
            boolean esReservaPropia = usuarioId != null && r.getUsuario().getUsuarioId().equals(usuarioId);

            // Comprobar si la reserva está en el pasado
            boolean esPasada = r.getFinReserva().isBefore(ahora);

            // Configurar propiedades según si es propia y si es pasada
            if (esReservaPropia) {
                if (esPasada) {
                    String title = "Mi reserva (Pasada)";
                    // Add participant information for pool reservations
                    if ("piscina"
                            .equalsIgnoreCase(r.getEspacioDeportivo().getServicioDeportivo().getServicioDeportivo())
                            && r.numeroParticipantes() != null
                            && r.numeroParticipantes() > 0) {
                        title += " (" + r.numeroParticipantes() + " participante" +
                                (r.numeroParticipantes() > 1 ? "s" : "") + ")";
                    }
                    eventoReserva.put("title", title);
                    eventoReserva.put("className", "evento-propio-pasado");
                    eventoReserva.put("backgroundColor", "#8f9bdb"); // Azul más claro/grisáceo
                    eventoReserva.put("borderColor", "#8f9bdb");
                    eventoReserva.put("textColor", "#555555");
                } else {
                    String title = "Mi reserva";
                    // Add participant information for pool reservations
                    if ("piscina"
                            .equalsIgnoreCase(r.getEspacioDeportivo().getServicioDeportivo().getServicioDeportivo())
                            && r.numeroParticipantes() != null
                            && r.numeroParticipantes() > 0) {
                        title += " (" + r.numeroParticipantes() + " participante" +
                                (r.numeroParticipantes() > 1 ? "s" : "") + ")";
                    }
                    eventoReserva.put("title", title);
                    eventoReserva.put("className", "evento-propio");
                    eventoReserva.put("backgroundColor", "#5664d2"); // Azul
                    eventoReserva.put("borderColor", "#5664d2");
                    eventoReserva.put("textColor", "#ffffff");
                }
            } else {
                if (esPasada) {
                    String title = "Reservado (Pasado)";
                    // Add participant information for pool reservations
                    if ("piscina"
                            .equalsIgnoreCase(r.getEspacioDeportivo().getServicioDeportivo().getServicioDeportivo())
                            && r.numeroParticipantes() != null
                            && r.numeroParticipantes() > 0) {
                        title += " (" + r.numeroParticipantes() + " participante" +
                                (r.numeroParticipantes() > 1 ? "s" : "") + ")";
                    }
                    eventoReserva.put("title", title);
                    eventoReserva.put("className", "evento-ajeno-pasado");
                    eventoReserva.put("backgroundColor", "#808080"); // Gris más oscuro
                    eventoReserva.put("borderColor", "#707070");
                    eventoReserva.put("textColor", "#444444");
                } else {
                    String title = "Reservado";
                    // Add participant information for pool reservations
                    if ("piscina"
                            .equalsIgnoreCase(r.getEspacioDeportivo().getServicioDeportivo().getServicioDeportivo())
                            && r.numeroParticipantes() != null
                            && r.numeroParticipantes() > 0) {
                        title += " (" + r.numeroParticipantes() + " participante" +
                                (r.numeroParticipantes() > 1 ? "s" : "") + ")";
                    }
                    eventoReserva.put("title", title);
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

            // Add participant information for pool reservations
            if ("piscina".equalsIgnoreCase(r.getEspacioDeportivo().getServicioDeportivo().getServicioDeportivo())) {
                eventoReserva.put("tipoServicio", "piscina");
                eventoReserva.put("numeroCarrilPiscina", r.getNumeroCarrilPiscina());
                eventoReserva.put("numeroParticipantes", r.numeroParticipantes());
            }

            eventos.add(eventoReserva);
        }

        // Obtener el espacio deportivo para saber horarios de operación
        Optional<EspacioDeportivo> optEspacio = espacioDeportivoRepository.findById(espacioId);
        if (optEspacio.isPresent()) {
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
            for (LocalDateTime diaActual = inicioRango.toLocalDate().atStartOfDay(); !diaActual.toLocalDate()
                    .isAfter(ahora.toLocalDate()); diaActual = diaActual.plusDays(1)) {

                // Para cada día, recorremos los slots de tiempo dentro del horario de operación
                LocalDateTime inicioSlot = diaActual.with(horaInicio);
                LocalDateTime finDia = diaActual.with(horaFin);

                // Si el día es hoy, solo generamos eventos hasta la hora actual
                LocalDateTime limiteFinal = diaActual.toLocalDate().equals(ahora.toLocalDate())
                        ? ahora
                        : finDia;

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
            HttpSession session) {
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

        // Agregar información de carriles si están disponibles
        if (reserva.getNumeroCarrilPiscina() != null) {
            detallesReserva.put("numeroCarrilPiscina", reserva.getNumeroCarrilPiscina());
        }
        if (reserva.getNumeroCarrilPista() != null) {
            detallesReserva.put("numeroCarrilPista", reserva.getNumeroCarrilPista());
        } else {
            // Devolver valor por defecto si es null para mantener consistencia
            detallesReserva.put("numeroCarrilPista", 0);
        }

        // Agregar número de participantes para piscina (siempre, usando valor por
        // defecto si es null)
        detallesReserva.put("numeroParticipantes",
                reserva.numeroParticipantes() != null ? reserva.numeroParticipantes() : 1);

        // Calcular duración en horas
        long duracionHoras = Duration.between(reserva.getInicioReserva(), reserva.getFinReserva()).toHours();
        detallesReserva.put("duracionHoras", duracionHoras);

        // Calcular precio total
        BigDecimal precioTotal = reserva.getEspacioDeportivo().getPrecioPorHora()
                .multiply(BigDecimal.valueOf(duracionHoras));

        // Si es piscina y tiene número de participantes, multiplicar por ese número
        if ("piscina".equalsIgnoreCase(reserva.getEspacioDeportivo().getServicioDeportivo().getServicioDeportivo())
                && reserva.numeroParticipantes() != null
                && reserva.numeroParticipantes() > 0) {
            precioTotal = precioTotal.multiply(BigDecimal.valueOf(reserva.numeroParticipantes()));
        }

        detallesReserva.put("precioTotal", precioTotal);

        // Información del espacio deportivo
        EspacioDeportivo espacioDeportivo = reserva.getEspacioDeportivo();
        detallesReserva.put("espacioDeportivo", espacioDeportivo.getNombre());
        detallesReserva.put("tipoServicio", espacioDeportivo.getServicioDeportivo().getServicioDeportivo());
        detallesReserva.put("espacioDeportivoId", espacioDeportivo.getEspacioDeportivoId());
        detallesReserva.put("establecimiento",
                espacioDeportivo.getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre());

        // Agregar aforo del gimnasio si corresponde
        if ("gimnasio".equalsIgnoreCase(espacioDeportivo.getServicioDeportivo().getServicioDeportivo())) {
            detallesReserva.put("aforoGimnasio",
                    espacioDeportivo.getAforoGimnasio() != null ? espacioDeportivo.getAforoGimnasio() : 0);
        }

        // Agregar capacidad de la piscina si corresponde
        if ("piscina".equalsIgnoreCase(espacioDeportivo.getServicioDeportivo().getServicioDeportivo())) {
            detallesReserva.put("maxPersonasPorCarril",
                    espacioDeportivo.getMaxPersonasPorCarril() != null ? espacioDeportivo.getMaxPersonasPorCarril()
                            : 0);
            detallesReserva.put("carrilesPiscina",
                    espacioDeportivo.getCarrilesPiscina() != null ? espacioDeportivo.getCarrilesPiscina() : 0);
        }

        return ResponseEntity.ok(detallesReserva);
    }

    @PostMapping("/procesar-reserva")
    public String procesarReserva(@RequestParam("reservaId") Integer reservaId,
            @RequestParam("espacioId") Integer espacioId,
            @RequestParam("cantidadPersonas") Integer cantidadPersonas,
            @RequestParam(value = "payment-method", defaultValue = "online") String metodoPago,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            redirectAttributes.addFlashAttribute("error", "Por favor, inicia sesión para continuar.");
            return "redirect:/usuarios/inicio";
        }

        try {
            // Obtener la reserva existente
            Reserva reserva = reservaRepository.findById(reservaId)
                    .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

            // Verificar que la reserva pertenece al usuario actual
            if (!reserva.getUsuario().getUsuarioId().equals(usuario.getUsuarioId())) {
                redirectAttributes.addFlashAttribute("error", "No tienes permisos para procesar esta reserva.");
                return "redirect:/usuarios/mis-reservas";
            }

            // Verificar que la reserva está en estado pendiente
            if (reserva.getEstado() != Reserva.Estado.pendiente) {
                redirectAttributes.addFlashAttribute("error", "Esta reserva ya ha sido procesada.");
                return "redirect:/usuarios/mis-reservas";
            }

            // Final validation - check for conflicts with other active reservations
            long conflictos = reservaRepository.countActiveReservationConflicts(
                    espacioId, reserva.getInicioReserva(), reserva.getFinReserva());
            if (conflictos > 1) { // > 1 porque la reserva actual cuenta como 1
                redirectAttributes.addFlashAttribute("error",
                        "Conflicto detectado: ya existe otra reserva en este horario.");
                return "redirect:/usuarios/calendario/" + espacioId;
            }

            // Actualizar la reserva con información adicional si es necesario
            // Por ahora, la cantidad de personas no se guarda en la entidad Reserva
            // pero se podría agregar en el futuro si es necesario

            // Proceder al pago dependiendo del método seleccionado
            if ("online".equals(metodoPago)) {
                // Redirigir al proceso de pago en línea
                return "redirect:/usuarios/pagar/" + reserva.getReservaId();
            } else {
                // Para otros métodos de pago en el futuro
                redirectAttributes.addFlashAttribute("error", "Método de pago no soportado actualmente.");
                return "redirect:/usuarios/reservar/" + espacioId;
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al procesar la reserva: " + e.getMessage());

            return "redirect:/usuarios/reservar/" + espacioId;
        }
    }

    @PostMapping("/crear-reserva-calendario")
    public String crearReservaDesdeCalendario(@RequestParam("espacioId") Integer espacioId,
            @RequestParam("fechaInicio") String fechaInicio,
            @RequestParam("fechaFin") String fechaFin,
            @RequestParam(value = "numeroCarrilPiscina", required = false) Integer numeroCarrilPiscina,
            @RequestParam(value = "numeroCarrilPista", required = false) Integer numeroCarrilPista,
            @RequestParam(value = "numeroParticipantes", required = false, defaultValue = "1") Integer numeroParticipantes,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            redirectAttributes.addFlashAttribute("error", "Por favor, inicia sesión para continuar.");
            return "redirect:/usuarios/inicio";
        }
        try {
            EspacioDeportivo espacio = espacioDeportivoRepository.findById(espacioId)
                    .orElseThrow(() -> new RuntimeException("Espacio deportivo no encontrado"));
            if (espacio.getEstadoServicio() != EspacioDeportivo.EstadoServicio.operativo) {
                redirectAttributes.addFlashAttribute("error", "El espacio deportivo no está disponible");
                return "redirect:/usuarios/reservasCalendario/" + espacioId;
            }
            LocalDateTime inicioReserva;
            LocalDateTime finReserva;
            try {
                inicioReserva = parsearFechaISOManteniendomHora(fechaInicio);
                finReserva = parsearFechaISOManteniendomHora(fechaFin);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Formato de fecha inválido");
                return "redirect:/usuarios/reservasCalendario/" + espacioId;
            }
            // Validaciones comunes de fechas
            if (!inicioReserva.isBefore(finReserva)) {
                redirectAttributes.addFlashAttribute("error", "La hora de inicio debe ser anterior a la hora de fin");
                return "redirect:/usuarios/reservasCalendario/" + espacioId;
            }
            if (inicioReserva.isBefore(LocalDateTime.now())) {
                redirectAttributes.addFlashAttribute("error", "No se pueden hacer reservas en horarios pasados");
                return "redirect:/usuarios/reservasCalendario/" + espacioId;
            }
            long duracionHoras = Duration.between(inicioReserva, finReserva).toHours();
            if (duracionHoras > 3) {
                redirectAttributes.addFlashAttribute("error", "La duración máxima de una reserva es de 3 horas");
                return "redirect:/usuarios/reservasCalendario/" + espacioId;
            }

            // Verificación de conflictos dependiendo del tipo de espacio deportivo
            if ("piscina".equalsIgnoreCase(espacio.getServicioDeportivo().getServicioDeportivo())) {
                // Lógica existente para piscinas...
                if (numeroCarrilPiscina == null) {
                    redirectAttributes.addFlashAttribute("error",
                            "Debe seleccionar un carril para la piscina");
                    return "redirect:/usuarios/reservasCalendario/" + espacioId;
                }
                List<Reserva> reservasCarril = reservaRepository.findActiveReservationsForLane(
                        espacioId, inicioReserva, finReserva, numeroCarrilPiscina);

                int participantesExistentes = 0;
                for (Reserva r : reservasCarril) {
                    participantesExistentes += (r.numeroParticipantes() != null)
                            ? r.numeroParticipantes()
                            : 1;
                }

                int espaciosDisponibles = espacio.getMaxPersonasPorCarril() - participantesExistentes;
                if (numeroParticipantes > espaciosDisponibles) {
                    redirectAttributes.addFlashAttribute("error",
                            "No hay suficiente espacio en este carril. Espacios disponibles: " + espaciosDisponibles);
                    return "redirect:/usuarios/reservasCalendario/" + espacioId;
                }
            } else if ("atletismo".equalsIgnoreCase(espacio.getServicioDeportivo().getServicioDeportivo())) {
                // Lógica para pistas de atletismo (similar a piscinas)
                if (numeroCarrilPista == null) {
                    redirectAttributes.addFlashAttribute("error",
                            "Debe seleccionar un carril para la pista de atletismo");
                    return "redirect:/usuarios/reservasCalendario/" + espacioId;
                }

                // Buscar reservas activas que usen el mismo carril en el mismo horario
                List<Reserva> reservasCarril = reservaRepository
                        .findActiveReservationsForLanePista(espacioId, inicioReserva, finReserva, numeroCarrilPista);

                // Contar participantes existentes en ese carril
                int participantesExistentes = 0;
                for (Reserva r : reservasCarril) {
                    participantesExistentes += (r.numeroParticipantes() != null)
                            ? r.numeroParticipantes()
                            : 1;
                }

                // Verificar si hay espacios disponibles
                int maxPersonasPorCarril = espacio.getMaxPersonasPorCarril() != null ? espacio.getMaxPersonasPorCarril()
                        : 1;
                int espaciosDisponibles = maxPersonasPorCarril - participantesExistentes;

                if (numeroParticipantes > espaciosDisponibles) {
                    redirectAttributes.addFlashAttribute("error",
                            "No hay suficiente espacio en este carril. Espacios disponibles: " + espaciosDisponibles);
                    return "redirect:/usuarios/reservasCalendario/" + espacioId;
                }
            } else if ("gimnasio".equalsIgnoreCase(espacio.getServicioDeportivo().getServicioDeportivo())) {
                // MODIFICACIÓN: Para gimnasios, verificar si hay suficiente espacio para los
                // participantes solicitados
                int aforoGimnasio = espacio.getAforoGimnasio();

                // Filtrar solo reservas activas (no canceladas)
                List<Reserva> reservasActivas = reservaRepository.findByEspacioDeportivo_EspacioDeportivoId(espacioId)
                        .stream()
                        .filter(r -> r.getInicioReserva().isBefore(finReserva)
                                && r.getFinReserva().isAfter(inicioReserva)
                                && r.getEstado() != null
                                && !r.getEstado().name().equalsIgnoreCase("CANCELADA"))
                        .collect(Collectors.toList());

                // Sumar todos los participantes de las reservas existentes
                int participantesExistentes = 0;
                for (Reserva r : reservasActivas) {
                    participantesExistentes += (r.getNumeroParticipantes() != null)
                            ? r.getNumeroParticipantes()
                            : 1;
                }

                // Calcular espacios disponibles
                int espaciosDisponibles = aforoGimnasio - participantesExistentes;

                // Verificar si hay suficiente espacio para los nuevos participantes
                if (numeroParticipantes > espaciosDisponibles) {
                    redirectAttributes.addFlashAttribute("error",
                            "No hay suficiente espacio en el gimnasio. Espacios disponibles: " + espaciosDisponibles);
                    return "redirect:/usuarios/reservasCalendario/" + espacioId;
                }
            } else {
                // Lógica existente para otros espacios...
                long conflictos = reservaRepository.countActiveReservationConflicts(espacioId, inicioReserva,
                        finReserva);
                if (conflictos > 0) {
                    redirectAttributes.addFlashAttribute("error",
                            "Ya existe otra reserva activa en este horario. Por favor, selecciona otro horario.");
                    return "redirect:/usuarios/reservasCalendario/" + espacioId;
                }
            }

            // Creación de la reserva
            Reserva nuevaReserva = new Reserva();
            nuevaReserva.setUsuario(usuario);
            nuevaReserva.setEspacioDeportivo(espacio);
            nuevaReserva.setInicioReserva(inicioReserva);
            nuevaReserva.setFinReserva(finReserva);
            nuevaReserva.setEstado(Reserva.Estado.pendiente);
            nuevaReserva.setFechaCreacion(LocalDateTime.now());

            // Asignar carril para piscina si está definido
            if (numeroCarrilPiscina != null) {
                nuevaReserva.setNumeroCarrilPiscina(numeroCarrilPiscina);
            }

            // Asignar carril para atletismo si está definido
            if (numeroCarrilPista != null) {
                nuevaReserva.setNumeroCarrilPista(numeroCarrilPista);
            }

            // Asignar número de participantes para piscina
            if ("piscina".equalsIgnoreCase(espacio.getServicioDeportivo().getServicioDeportivo())) {
                nuevaReserva.setNumeroParticipantes(numeroParticipantes != null ? numeroParticipantes : 1);
            }

            // Asignar número de participantes para gimnasio
            if ("gimnasio".equalsIgnoreCase(espacio.getServicioDeportivo().getServicioDeportivo())) {
                nuevaReserva.setNumeroParticipantes(numeroParticipantes != null ? numeroParticipantes : 1);
            }

            // Asignar número de participantes para atletismo
            if ("atletismo".equalsIgnoreCase(espacio.getServicioDeportivo().getServicioDeportivo())) {
                nuevaReserva.setNumeroParticipantes(numeroParticipantes != null ? numeroParticipantes : 1);
            }

            Reserva reservaGuardada = reservaRepository.save(nuevaReserva);
            return "redirect:/usuarios/confirmarReserva/" + reservaGuardada.getReservaId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al crear la reserva: " + e.getMessage());
            return "redirect:/usuarios/reservasCalendario/" + espacioId;
        }
    }

    @GetMapping("/confirmarReserva/{reservaId}")
    public String verDetalleReserva(@PathVariable("reservaId") Integer reservaId, Model model, HttpSession session,
            RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            redirectAttributes.addFlashAttribute("error", "Por favor, inicia sesión para continuar.");
            return "redirect:/usuarios/inicio";
        }
        Optional<Reserva> optReserva = reservaRepository.findById(reservaId);
        if (optReserva.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Reserva no encontrada");
            return "redirect:/usuarios/mis-reservas";
        }
        Reserva reserva = optReserva.get();
        if (!reserva.getUsuario().getUsuarioId().equals(usuario.getUsuarioId())) {
            redirectAttributes.addFlashAttribute("error", "No tienes permiso para ver esta reserva");
            return "redirect:/usuarios/mis-reservas";
        }

        if (!reserva.getEstado().equals(Reserva.Estado.pendiente)) {
            redirectAttributes.addFlashAttribute("error", "La reserva no está pendiente de pago");
            return "redirect:/usuarios/mis-reservas";
        }

        long duracionHoras = java.time.Duration.between(reserva.getInicioReserva(), reserva.getFinReserva()).toHours();

        // Calcular precio base (precio por hora × horas)
        BigDecimal precioBase = reserva.getEspacioDeportivo().getPrecioPorHora()
                .multiply(BigDecimal.valueOf(duracionHoras));

        // Calcular precio total según tipo de servicio
        BigDecimal precioTotal = precioBase;

        // Si es piscina, gimnasio o atletismo y tiene número de participantes,
        // multiplicar por ese número
        Integer numeroParticipantes = 1; // Valor por defecto
        String tipoServicio = reserva.getEspacioDeportivo().getServicioDeportivo().getServicioDeportivo();

        if (("piscina".equalsIgnoreCase(tipoServicio) ||
                "gimnasio".equalsIgnoreCase(tipoServicio) ||
                "atletismo".equalsIgnoreCase(tipoServicio))
                && reserva.numeroParticipantes() != null && reserva.numeroParticipantes() > 0) {
            numeroParticipantes = reserva.numeroParticipantes();
            precioTotal = precioBase.multiply(BigDecimal.valueOf(numeroParticipantes));
        }

        model.addAttribute("reserva", reserva);
        model.addAttribute("espacio", reserva.getEspacioDeportivo());
        model.addAttribute("usuario", usuario);
        model.addAttribute("duracionHoras", duracionHoras);
        model.addAttribute("numeroParticipantes", numeroParticipantes);
        model.addAttribute("precioTotal", precioTotal);

        return "Vecino/detalle-reserva";
    }

    // Método auxiliar para parsear fechas ISO manteniendo la hora exacta
    private LocalDateTime parsearFechaISOManteniendomHora(String fechaISO) {
        // Manejar formato ISO con zona horaria (ej: "2025-06-11T16:00:00.000Z")
        if (fechaISO.endsWith("Z")) {
            // Remover la Z y milisegundos, mantener la hora exacta como local
            String fechaSinZ = fechaISO.substring(0, fechaISO.length() - 1);
            if (fechaSinZ.contains(".")) {
                fechaSinZ = fechaSinZ.substring(0, fechaSinZ.indexOf('.'));
            }
            return LocalDateTime.parse(fechaSinZ);
        } else {
            return LocalDateTime.parse(fechaISO);
        }
    }

    @GetMapping("/calendario/{espacioId}")
    public String mostrarCalendario(@PathVariable("espacioId") Integer espacioId, Model model,
            RedirectAttributes redirectAttributes) {
        Optional<EspacioDeportivo> optEspacio = espacioDeportivoRepository.findById(espacioId);
        if (optEspacio.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Espacio deportivo no encontrado");
            return "redirect:/usuarios/mis-reservas";
        }

        EspacioDeportivo espacioDeportivo = optEspacio.get();
        model.addAttribute("espacioId", espacioId);
        model.addAttribute("espacio", espacioDeportivo);
        // Si es gimnasio, pasar aforo_gimnasio al modelo
        if ("gimnasio".equalsIgnoreCase(espacioDeportivo.getServicioDeportivo().getServicioDeportivo())) {
            model.addAttribute("aforoGimnasio", espacioDeportivo.getAforoGimnasio());
        }
        return "Vecino/vecino-calendario";
    }
}
