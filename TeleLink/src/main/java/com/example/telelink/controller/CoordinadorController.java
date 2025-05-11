    package com.example.telelink.controller;

    import com.example.telelink.entity.*;
    import com.example.telelink.repository.*;
    import jakarta.servlet.http.HttpSession;
    import jakarta.validation.Valid;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.format.annotation.DateTimeFormat;
    import org.springframework.http.ResponseEntity;
    import org.springframework.stereotype.Controller;
    import org.springframework.ui.Model;
    import org.springframework.validation.BindingResult;
    import org.springframework.validation.FieldError;
    import org.springframework.web.bind.annotation.*;
    import org.springframework.web.multipart.MultipartFile;
    import org.springframework.web.servlet.mvc.support.RedirectAttributes;

    import java.security.Timestamp;
    import java.text.DecimalFormat;
    import java.time.LocalDateTime;
    import java.time.format.DateTimeFormatter;
    import java.util.HashMap;
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

        @Autowired
        private ReseniaRepository reseniaRepository;

        @Autowired
        private ObservacionRepository observacionRepository;

        @GetMapping("/inicio")
        public String mostrarInicio(Model model, HttpSession session) {
            Usuario usuario = usuarioRepository.findFirstCoordinadorByOrderByUsuarioIdAsc()
                    .orElseThrow(() -> new IllegalArgumentException("No se encontró ningún coordinador en la base de datos"));

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
            if (usuario == null) {
                throw new IllegalArgumentException("Usuario no encontrado en la sesión");
            }
            model.addAttribute("usuario", usuario);

            List<Asistencia> asistencias = asistenciaRepository.findByCoordinador_UsuarioId(usuario.getUsuarioId());
            model.addAttribute("asistencias", asistencias);

            LocalDateTime ahora = LocalDateTime.now();
            Asistencia asistenciaActiva = null;
            for (Asistencia asistencia : asistencias) {
                LocalDateTime inicioRango = asistencia.getHorarioEntrada().minusMinutes(15);
                LocalDateTime finRango = asistencia.getHorarioSalida().plusMinutes(15);
                if (ahora.isAfter(inicioRango) && ahora.isBefore(finRango)) {
                    asistenciaActiva = asistencia;
                    break;
                }
            }

            if (asistenciaActiva != null) {
                DateTimeFormatter formatterHora = DateTimeFormatter.ofPattern("h:mm a");
                String horario = asistenciaActiva.getHorarioEntrada().format(formatterHora) + " - " +
                        asistenciaActiva.getHorarioSalida().format(formatterHora);

                String establecimiento = asistenciaActiva.getEspacioDeportivo()
                        .getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre();
                String servicioDeportivo = asistenciaActiva.getEspacioDeportivo()
                        .getServicioDeportivo().getServicioDeportivo();
                String geolocalizacion = asistenciaActiva.getEspacioDeportivo().getGeolocalizacion();

                model.addAttribute("asistenciaActiva", asistenciaActiva);
                model.addAttribute("horario", horario);
                model.addAttribute("establecimiento", establecimiento);
                model.addAttribute("servicioDeportivo", servicioDeportivo);
                model.addAttribute("geolocalizacion", geolocalizacion);
            } else {
                model.addAttribute("asistenciaActiva", null);
                model.addAttribute("horario", "--");
                model.addAttribute("establecimiento", "--");
                model.addAttribute("servicioDeportivo", "--");
                model.addAttribute("geolocalizacion", null);
            }

            return "Coordinador/asistencia";
        }

        // Fórmula de Haversine
        private double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
            final int R = 6371; // Radio de la Tierra en kilómetros
            double latDistance = Math.toRadians(lat2 - lat1);
            double lonDistance = Math.toRadians(lon2 - lon1);
            double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                    + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                    * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            double distance = R * c * 1000; // Convertir a metros
            return distance;
        }

        @PostMapping("/registrar-entrada")
        public String registrarEntrada(
                @RequestParam("latitude") Double latitude,
                @RequestParam("longitude") Double longitude,
                HttpSession session,
                RedirectAttributes redirectAttributes) {
            try {
                Usuario usuario = (Usuario) session.getAttribute("currentUser");
                if (usuario == null) {
                    redirectAttributes.addFlashAttribute("message", "Usuario no encontrado en la sesión");
                    redirectAttributes.addFlashAttribute("messageType", "error");
                    return "redirect:/coordinador/asistencia";
                }

                LocalDateTime ahora = LocalDateTime.now();
                List<Asistencia> asistencias = asistenciaRepository.findByCoordinador_UsuarioId(usuario.getUsuarioId());
                Asistencia asistenciaActiva = null;
                for (Asistencia asistencia : asistencias) {
                    LocalDateTime inicioRango = asistencia.getHorarioEntrada().minusMinutes(15);
                    LocalDateTime finRango = asistencia.getHorarioSalida().plusMinutes(15);
                    if (ahora.isAfter(inicioRango) && ahora.isBefore(finRango)) {
                        asistenciaActiva = asistencia;
                        break;
                    }
                }

                if (asistenciaActiva == null) {
                    redirectAttributes.addFlashAttribute("message", "No hay asistencia activa para registrar entrada");
                    redirectAttributes.addFlashAttribute("messageType", "error");
                    return "redirect:/coordinador/asistencia";
                }

                // Validar tiempo
                LocalDateTime limiteEntrada = asistenciaActiva.getHorarioEntrada().minusMinutes(15);
                if (ahora.isBefore(limiteEntrada)) {
                    redirectAttributes.addFlashAttribute("message", "No se puede registrar entrada antes de " + limiteEntrada.format(DateTimeFormatter.ofPattern("h:mm a")));
                    redirectAttributes.addFlashAttribute("messageType", "error");
                    return "redirect:/coordinador/asistencia";
                }

                if (asistenciaActiva.getRegistroEntrada() != null) {
                    redirectAttributes.addFlashAttribute("message", "La entrada ya ha sido registrada");
                    redirectAttributes.addFlashAttribute("messageType", "error");
                    return "redirect:/coordinador/asistencia";
                }

                // Validar geolocalización
                String geolocalizacion = asistenciaActiva.getEspacioDeportivo().getGeolocalizacion();
                if (geolocalizacion == null || !geolocalizacion.matches("^-?\\d+\\.\\d+\\s*,\\s*-?\\d+\\.\\d+$")) {
                    redirectAttributes.addFlashAttribute("message", "Geolocalización del espacio deportivo no disponible o inválida");
                    redirectAttributes.addFlashAttribute("messageType", "error");
                    return "redirect:/coordinador/asistencia";
                }

                String[] coords = geolocalizacion.split(",");
                double latEspacio = Double.parseDouble(coords[0].trim());
                double lngEspacio = Double.parseDouble(coords[1].trim());

                if (latitude == null || longitude == null) {
                    redirectAttributes.addFlashAttribute("message", "No se proporcionaron coordenadas válidas");
                    redirectAttributes.addFlashAttribute("messageType", "error");
                    return "redirect:/coordinador/asistencia";
                }

                double distancia = calcularDistancia(latitude, longitude, latEspacio, lngEspacio);
                if (distancia > 50) {
                    redirectAttributes.addFlashAttribute("message", "No estás en el espacio deportivo. Debes estar a menos de 50 metros para registrar tu entrada.");
                    redirectAttributes.addFlashAttribute("messageType", "error");
                    return "redirect:/coordinador/asistencia";
                }

                // Registrar entrada y actualizar estado
                asistenciaActiva.setRegistroEntrada(ahora);
                if (ahora.compareTo(asistenciaActiva.getHorarioEntrada()) <= 0) {
                    asistenciaActiva.setEstadoEntrada(Asistencia.EstadoEntrada.puntual);
                } else {
                    asistenciaActiva.setEstadoEntrada(Asistencia.EstadoEntrada.tarde);
                }
                asistenciaRepository.save(asistenciaActiva);

                redirectAttributes.addFlashAttribute("message", "Entrada registrada exitosamente a las " + ahora.format(DateTimeFormatter.ofPattern("h:mm a")));
                redirectAttributes.addFlashAttribute("messageType", "success");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("message", "Error al registrar entrada: " + e.getMessage());
                redirectAttributes.addFlashAttribute("messageType", "error");
            }
            return "redirect:/coordinador/asistencia";
        }

        @PostMapping("/registrar-salida")
        public String registrarSalida(
                @RequestParam("latitude") Double latitude,
                @RequestParam("longitude") Double longitude,
                HttpSession session,
                RedirectAttributes redirectAttributes) {
            try {
                Usuario usuario = (Usuario) session.getAttribute("currentUser");
                if (usuario == null) {
                    redirectAttributes.addFlashAttribute("message", "Usuario no encontrado en la sesión");
                    redirectAttributes.addFlashAttribute("messageType", "error");
                    return "redirect:/coordinador/asistencia";
                }

                LocalDateTime ahora = LocalDateTime.now();
                List<Asistencia> asistencias = asistenciaRepository.findByCoordinador_UsuarioId(usuario.getUsuarioId());
                Asistencia asistenciaActiva = null;
                for (Asistencia asistencia : asistencias) {
                    LocalDateTime inicioRango = asistencia.getHorarioEntrada().minusMinutes(15);
                    LocalDateTime finRango = asistencia.getHorarioSalida().plusMinutes(15);
                    if (ahora.isAfter(inicioRango) && ahora.isBefore(finRango)) {
                        asistenciaActiva = asistencia;
                        break;
                    }
                }

                if (asistenciaActiva == null) {
                    redirectAttributes.addFlashAttribute("message", "No hay asistencia activa para registrar salida");
                    redirectAttributes.addFlashAttribute("messageType", "error");
                    return "redirect:/coordinador/asistencia";
                }

                // Validar tiempo
                LocalDateTime limiteSalida = asistenciaActiva.getHorarioSalida().plusMinutes(15);
                if (ahora.isAfter(limiteSalida)) {
                    redirectAttributes.addFlashAttribute("message", "No se puede registrar salida después de " + limiteSalida.format(DateTimeFormatter.ofPattern("h:mm a")));
                    redirectAttributes.addFlashAttribute("messageType", "error");
                    return "redirect:/coordinador/asistencia";
                }

                if (asistenciaActiva.getRegistroEntrada() == null) {
                    redirectAttributes.addFlashAttribute("message", "Debe registrar la entrada antes de registrar la salida");
                    redirectAttributes.addFlashAttribute("messageType", "error");
                    return "redirect:/coordinador/asistencia";
                }

                if (asistenciaActiva.getRegistroSalida() != null) {
                    redirectAttributes.addFlashAttribute("message", "La salida ya ha sido registrada");
                    redirectAttributes.addFlashAttribute("messageType", "error");
                    return "redirect:/coordinador/asistencia";
                }

                // Validar geolocalización
                String geolocalizacion = asistenciaActiva.getEspacioDeportivo().getGeolocalizacion();
                if (geolocalizacion == null || !geolocalizacion.matches("^-?\\d+\\.\\d+\\s*,\\s*-?\\d+\\.\\d+$")) {
                    redirectAttributes.addFlashAttribute("message", "Geolocalización del espacio deportivo no disponible o inválida");
                    redirectAttributes.addFlashAttribute("messageType", "error");
                    return "redirect:/coordinador/asistencia";
                }

                String[] coords = geolocalizacion.split(",");
                double latEspacio = Double.parseDouble(coords[0].trim());
                double lngEspacio = Double.parseDouble(coords[1].trim());

                if (latitude == null || longitude == null) {
                    redirectAttributes.addFlashAttribute("message", "No se proporcionaron coordenadas válidas");
                    redirectAttributes.addFlashAttribute("messageType", "error");
                    return "redirect:/coordinador/asistencia";
                }

                double distancia = calcularDistancia(latitude, longitude, latEspacio, lngEspacio);
                if (distancia > 50) {
                    redirectAttributes.addFlashAttribute("message", "No estás en el espacio deportivo. Debes estar a menos de 50 metros para registrar tu salida.");
                    redirectAttributes.addFlashAttribute("messageType", "error");
                    return "redirect:/coordinador/asistencia";
                }

                // Registrar salida
                asistenciaActiva.setRegistroSalida(ahora);
                asistenciaRepository.save(asistenciaActiva);

                redirectAttributes.addFlashAttribute("message", "Salida registrada exitosamente a las " + ahora.format(DateTimeFormatter.ofPattern("h:mm a")));
                redirectAttributes.addFlashAttribute("messageType", "success");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("message", "Error al registrar salida: " + e.getMessage());
                redirectAttributes.addFlashAttribute("messageType", "error");
            }
            return "redirect:/coordinador/asistencia";
        }

        @GetMapping("/notificaciones")
        public String mostrarNotificaciones(Model model, HttpSession session) {
            Usuario usuario = (Usuario) session.getAttribute("currentUser");
            model.addAttribute("usuario", usuario);
            return "Coordinador/notificaciones";
        }

        @GetMapping("/perfil")
        public String mostrarPerfil(Model model, HttpSession session) {
            Usuario usuario = (Usuario) session.getAttribute("currentUser");
            model.addAttribute("usuario", usuario);
            return "Coordinador/perfil";
        }

        @GetMapping("/editar-perfil")
        public String mostrarEditarPerfil(@ModelAttribute("usuario") Usuario usuarioActualizado, Model model, HttpSession session) {
            Usuario usuario = (Usuario) session.getAttribute("currentUser");
            model.addAttribute("usuario", usuario);
            return "Coordinador/editarPerfil";
        }

        @PostMapping("/actualizar-perfil")
        public String actualizarPerfil(@RequestParam("fotoPerfil") MultipartFile fotoPerfil, @ModelAttribute("usuario") Usuario usuarioActualizado, HttpSession session) {
            Usuario usuario = (Usuario) session.getAttribute("currentUser");

            // Actualizar solo los campos permitidos (teléfono y foto)
            usuario.setTelefono(usuarioActualizado.getTelefono());

            // Codigo provisional para cargar una imagen
            usuario.setFotoPerfilUrl("https://img.freepik.com/foto-gratis/disparo-cabeza-hombre-atractivo-sonriendo-complacido-mirando-intrigado-pie-sobre-fondo-azul_1258-65733.jpg");

            // Guardar los cambios en la base de datos
            usuarioRepository.save(usuario);

            // Actualizar el objeto en la sesión
            session.setAttribute("currentUser", usuario);

            return "redirect:/coordinador/perfil";
        }

        private String obtenerClaseEstado(String estado) {
            if ("operativo".equalsIgnoreCase(estado)) {
                return "bg-success";
            } else if ("mantenimiento".equalsIgnoreCase(estado)) {
                return "bg-warning";
            } else if ("clausurado".equalsIgnoreCase(estado)) {
                return "bg-danger";
            } else {
                return "bg-secondary";
            }
        }

        @GetMapping("/espacios-deportivos")
        public String mostrarEspaciosDeportivos(Model model, HttpSession session) {
            Usuario usuario = (Usuario) session.getAttribute("currentUser");
            model.addAttribute("usuario", usuario);

            List<EspacioDeportivo> espacios = espacioDeportivoRepository.findAll();
            model.addAttribute("espacios", espacios);

            // Crear un mapa con los colores
            Map<Integer, String> clasesEstado = new HashMap<>();
            for (EspacioDeportivo espacio : espacios) {
                String clase = obtenerClaseEstado(espacio.getEstadoServicio().name());
                clasesEstado.put(espacio.getEspacioDeportivoId(), clase);
            }
            model.addAttribute("clasesEstado", clasesEstado);

            return "Coordinador/espaciosDeportivos";
        }


        @GetMapping("/espacioDetalle")
        public String mostrarDetalleEspacio(@RequestParam("espacioId") Integer espacioId, Model model, HttpSession session) {
            Usuario usuario = (Usuario) session.getAttribute("currentUser");
            model.addAttribute("usuario", usuario);

            EspacioDeportivo espacio = espacioDeportivoRepository.findById(espacioId)
                    .orElseThrow(() -> new IllegalArgumentException("Espacio no encontrado"));
            model.addAttribute("espacio", espacio);

            List<Resenia> resenias = reseniaRepository.findByEspacioDeportivo_EspacioDeportivoId(espacioId);
            double promedioCalificacion = resenias.isEmpty() ? 0.0 : resenias.stream()
                    .mapToInt(Resenia::getCalificacion)
                    .average()
                    .orElse(0.0);
            model.addAttribute("promedioCalificacion", promedioCalificacion);

            // Formatear promedioCalificacion
            DecimalFormat df = new DecimalFormat("#.#");
            String formattedPromedioCalificacion = df.format(promedioCalificacion);
            model.addAttribute("formattedPromedioCalificacion", formattedPromedioCalificacion + " / 5.0");

            // Formatear precioPorHora
            String formattedPrecioPorHora = espacio.getPrecioPorHora() != null ?
                    "S/ " + new DecimalFormat("#.##").format(espacio.getPrecioPorHora()) :
                    "S/ 0.00";
            model.addAttribute("formattedPrecioPorHora", formattedPrecioPorHora);

            /*/ Agregar la clase del estado al modelo
            String estadoClase = obtenerClaseEstado(espacio.getEstadoServicio().name());
            model.addAttribute("estadoClase", estadoClase);*/

            return "Coordinador/espacioDetalle";
        }

        // Listar todas las observaciones del coordinador
        @GetMapping("/observaciones")
        public String mostrarObservaciones(Model model, HttpSession session) {
            Usuario usuario = (Usuario) session.getAttribute("currentUser");
            if (usuario == null) {
                throw new IllegalArgumentException("Usuario no encontrado en la sesión");
            }
            List<Observacion> observaciones = observacionRepository.findByCoordinador_UsuarioId(usuario.getUsuarioId());
            model.addAttribute("observaciones", observaciones);
            model.addAttribute("usuario", usuario);
            return "Coordinador/observaciones";
        }

        // Mostrar los detalles de una observación
        @GetMapping("/observacionDetalle")
        public String mostrarObservacionDetalle(@RequestParam("observacionId") Integer observacionId, Model model, HttpSession session) {
            Usuario usuario = (Usuario) session.getAttribute("currentUser");
            if (usuario == null) {
                throw new IllegalArgumentException("Usuario no encontrado en la sesión");
            }
            Observacion observacion = observacionRepository.findById(observacionId)
                    .orElseThrow(() -> new IllegalArgumentException("Observación no encontrada"));
            model.addAttribute("observacion", observacion);
            model.addAttribute("usuario", usuario);
            String nivelUrgenciaCapitalizado = observacion.getNivelUrgencia().name().substring(0, 1).toUpperCase() +
                    observacion.getNivelUrgencia().name().substring(1).toLowerCase();
            model.addAttribute("nivelUrgenciaCapitalizado", nivelUrgenciaCapitalizado);
            return "Coordinador/observacionDetalle";
        }

        // Mostrar formulario para crear una nueva observación (desde asistencias)
        @GetMapping("/observacionNewForm")
        public String mostrarFormularioNuevaObservacion(
                @RequestParam(value = "asistenciaId", required = false) Integer asistenciaId,
                @ModelAttribute("observacionForm") Observacion observacionForm,
                BindingResult result,
                Model model,
                HttpSession session) {
            Usuario usuario = (Usuario) session.getAttribute("currentUser");
            if (usuario == null) {
                throw new IllegalArgumentException("Usuario no encontrado en la sesión");
            }
            model.addAttribute("usuario", usuario);

            // Inicializar observacionForm si es la primera carga (no viene de un error de validación)
            if (observacionForm.getEspacioDeportivo() == null && observacionForm.getNivelUrgencia() == null && observacionForm.getDescripcion() == null) {
                observacionForm = new Observacion();
                if (asistenciaId != null) {
                    Asistencia asistencia = asistenciaRepository.findById(asistenciaId)
                            .orElseThrow(() -> new IllegalArgumentException("Asistencia no encontrada"));
                    observacionForm.setEspacioDeportivo(asistencia.getEspacioDeportivo());
                    observacionForm.setCoordinador(usuario);
                    observacionForm.setFechaCreacion(LocalDateTime.now());
                }
                model.addAttribute("observacionForm", observacionForm);
            }

            // Añadir asistenciaId al modelo
            model.addAttribute("asistenciaId", asistenciaId);

            // Si hay errores de validación, asegúrate de que estén disponibles
            if (result.hasErrors()) {
                model.addAttribute("org.springframework.validation.BindingResult.observacionForm", result);
            }

            return "Coordinador/observacionNewForm";
        }

        // Mostrar formulario para editar una observación existente
        @GetMapping("/observacionEditForm")
        public String mostrarFormularioEditarObservacion(
                @RequestParam("observacionId") Integer observacionId,
                Model model,
                HttpSession session) {
            Usuario usuario = (Usuario) session.getAttribute("currentUser");
            if (usuario == null) {
                throw new IllegalArgumentException("Usuario no encontrado en la sesión");
            }
            model.addAttribute("usuario", usuario);

            Observacion observacion = observacionRepository.findById(observacionId)
                    .orElseThrow(() -> new IllegalArgumentException("Observación no encontrada"));
            if (!"pendiente".equals(observacion.getEstado().name())) {
                throw new IllegalArgumentException("Solo se pueden editar observaciones en estado 'pendiente'");
            }

            model.addAttribute("observacionForm", observacion);
            return "Coordinador/observacionEditForm";
        }

        // Guardar una observación (creación o edición)
        @PostMapping("/observacionGuardar")
        public String guardarObservacion(
                @Valid @ModelAttribute("observacionForm") Observacion observacionForm,
                BindingResult result,
                @RequestParam(value = "asistenciaId", required = false) Integer asistenciaId,
                @RequestParam(value = "foto", required = false) MultipartFile foto,
                HttpSession session,
                RedirectAttributes redirectAttributes,
                Model model) {
            Usuario usuario = (Usuario) session.getAttribute("currentUser");
            if (usuario == null) {
                redirectAttributes.addFlashAttribute("message", "Usuario no encontrado en la sesión");
                redirectAttributes.addFlashAttribute("messageType", "error");
                return "redirect:/coordinador/observaciones";
            }

            // Asignar valores requeridos antes de la validación
            observacionForm.setCoordinador(usuario);
            observacionForm.setFechaCreacion(LocalDateTime.now());
            if (asistenciaId != null) {
                Asistencia asistencia = asistenciaRepository.findById(asistenciaId)
                        .orElseThrow(() -> new IllegalArgumentException("Asistencia no encontrada"));
                observacionForm.setEspacioDeportivo(asistencia.getEspacioDeportivo());
            }

            // Manejo de la foto y asignación de fotoUrl antes de la validación
            boolean isCreation = observacionForm.getObservacionId() == null;
            if (isCreation && (foto != null && !foto.isEmpty())) {
                String fotoUrl = "https://example.com/fotos/observacion.jpg"; // URL predeterminada
                observacionForm.setFotoUrl(fotoUrl);
            }

            // Validar manualmente la foto al crear
            if (isCreation && (foto == null || foto.isEmpty())) {
                result.rejectValue("fotoUrl", "NotBlank", "La foto es obligatoria al crear una observación");
            }

            // Evaluar errores después de asignar los campos requeridos
            if (result.hasErrors()) {
                // Imprimir los errores en la consola para depuración
                System.out.println("Errores de validación encontrados:");
                result.getAllErrors().forEach(error -> {
                    System.out.println("Campo: " + error.getObjectName() + " - Mensaje: " + error.getDefaultMessage());
                    if (error instanceof FieldError) {
                        FieldError fieldError = (FieldError) error;
                        System.out.println("Campo específico: " + fieldError.getField());
                    }
                });

                // Añadir asistenciaId y usuario al modelo para renderizar el formulario
                model.addAttribute("asistenciaId", asistenciaId);
                model.addAttribute("usuario", usuario);
                model.addAttribute("observacionForm", observacionForm);
                model.addAttribute("org.springframework.validation.BindingResult.observacionForm", result);

                // Retornar directamente la vista del formulario
                return "Coordinador/observacionNewForm";
            }

            // Guardar la observación
            observacionRepository.save(observacionForm);
            redirectAttributes.addFlashAttribute("message", "Observación guardada exitosamente");
            redirectAttributes.addFlashAttribute("messageType", "success");
            return "redirect:/coordinador/observaciones";
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
