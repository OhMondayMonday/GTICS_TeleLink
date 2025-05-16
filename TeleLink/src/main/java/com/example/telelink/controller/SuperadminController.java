package com.example.telelink.controller;

import com.example.telelink.entity.*;

import com.example.telelink.repository.*;
import com.example.telelink.service.AvisoService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.telelink.service.S3Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/superadmin")
public class SuperadminController {

    @Autowired
    private ReservaRepository reservaRepository;
    @Autowired
    private AvisoRepository avisoRepository;
    @Autowired
    private PagoRepository pagoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private AvisoService avisoService;
    @Autowired
    private ReembolsoRepository reembolsoRepository;
    @Autowired
    private RolRepository rolRepository;
    @Autowired
    private TipoActividadRepository tipoActividadRepository;
    @Autowired
    private ActividadUsuarioRepository actividadUsuarioRepository;
    @Autowired
    private S3Service s3Service;

    @GetMapping("/inicio")
    public String dashboard(Model model, HttpSession session) {

        Usuario usuariologeado = (Usuario) session.getAttribute("usuario");

        if (usuariologeado != null) {
            model.addAttribute("userId", usuariologeado.getUsuarioId());
        }

        // Obtener los datos de reservas y pagos
        Integer numeroReservasMes = reservaRepository.numeroReservasMes();
        Integer numeroReservasMesPasado = reservaRepository.numeroReservasMesPasado();
        Double montoMensual = reservaRepository.obtenerMontoTotalDeReservasEsteMes();
        montoMensual = montoMensual == null ? 0 : montoMensual;
        Double promedioMensual = montoMensual/numeroReservasMes;
        promedioMensual =  montoMensual == 0.0 ? 0.0 : promedioMensual;
        Double montoMensualPasado = reservaRepository.obtenerMontoTotalDeReservasMesPasado();
        montoMensualPasado = (montoMensualPasado == null) ? 0.0 : montoMensualPasado;
        double promedioMensualPasado = montoMensualPasado/numeroReservasMesPasado;
        promedioMensualPasado = (montoMensualPasado == 0.0) ? 0.0 : promedioMensualPasado;
        List<Aviso> avisos = avisoRepository.obtenerUltimosAvisos();
        Integer reservasFutbol = reservaRepository.obtenerNumeroReservasPorServicio(2);
        Integer reservasPiscina = reservaRepository.obtenerNumeroReservasPorServicio(4);
        Integer reservasGimnasio = reservaRepository.obtenerNumeroReservasPorServicio(1);
        Integer reservasAtletismo = reservaRepository.obtenerNumeroReservasPorServicio(3);
        BigDecimal pagoPlinMensual = pagoRepository.obtenerMontoMensualPorMetodoPago(2); // ID para Plin
        BigDecimal pagoYapeMensual = pagoRepository.obtenerMontoMensualPorMetodoPago(1); // ID para Yape
        BigDecimal pagoIzipayMensual = pagoRepository.obtenerMontoMensualPorMetodoPago(3); // ID para Izipay
        BigDecimal pagoEfectivoMensual = pagoRepository.obtenerMontoMensualPorMetodoPago(4);
        BigDecimal pagoPlinSemanal = pagoRepository.obtenerMontoSemanalPorMetodoPago(2); // ID para Plin
        BigDecimal pagoYapeSemanal = pagoRepository.obtenerMontoSemanalPorMetodoPago(1); // ID para Yape
        BigDecimal pagoIzipaySemanal = pagoRepository.obtenerMontoSemanalPorMetodoPago(3); // ID para Izipay
        BigDecimal pagoEfectivoSemanal = pagoRepository.obtenerMontoSemanalPorMetodoPago(4); // ID para Efectivo
        List<Integer> chartData = Arrays.asList(reservasGimnasio, reservasFutbol, reservasAtletismo, reservasPiscina);

        // Calcular la diferencia en las reservas y definir el badge
        String badge;
        long diferencia = numeroReservasMes - numeroReservasMesPasado;
        if (diferencia < 0) {
            badge = "badge bg-danger-subtle text-danger font-size-11";
        } else {
            badge = "badge bg-success-subtle text-success font-size-11";
        }

        Aviso avisoActivo = avisoRepository.findByEstadoAviso("activo").stream()
                .filter(a -> !a.esEstadoDefault())  // Excluir el default
                .findFirst()
                .orElseGet(() -> avisoRepository.findByEstadoAviso("default")
                        .stream()
                        .findFirst()
                        .orElse(null));


        // Agregar los datos al modelo
        model.addAttribute("usuariologeado", usuariologeado);
        model.addAttribute("numeroReservasMes", numeroReservasMes);
        model.addAttribute("promedioMensualPasado", promedioMensualPasado);
        model.addAttribute("diferencia", diferencia);
        model.addAttribute("badge", badge);
        model.addAttribute("montoMensual", montoMensual);
        model.addAttribute("promedioMensual", promedioMensual);
        model.addAttribute("montoMensualPasado", montoMensualPasado);
        model.addAttribute("avisos", avisos);
        model.addAttribute("avisoActivo", avisoActivo);
        model.addAttribute("chartData", chartData);
        model.addAttribute("pagoPlinMensual", pagoPlinMensual);
        model.addAttribute("pagoYapeMensual", pagoYapeMensual);
        model.addAttribute("pagoIzipayMensual", pagoIzipayMensual);
        model.addAttribute("pagoEfectivoMensual", pagoEfectivoMensual);
        model.addAttribute("pagoPlinSemanal", pagoPlinSemanal);
        model.addAttribute("pagoYapeSemanal", pagoYapeSemanal);
        model.addAttribute("pagoIzipaySemanal", pagoIzipaySemanal);
        model.addAttribute("pagoEfectivoSemanal", pagoEfectivoSemanal);

        // Retornar la vista HTML
        return "Superadmin/Dashboard";  // Nombre de la vista (dashboard.html)
    }

    @GetMapping("/editarAviso/{id}")
    public String mostrarFormularioEdicion(@PathVariable Integer id, Model model) {
        Aviso aviso = avisoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Aviso no encontrado con ID: " + id));
        model.addAttribute("aviso", aviso);
        return "Superadmin/editarAviso";
    }

    @PostMapping("/actualizarAviso/{id}")
    public String actualizarAviso(@PathVariable Integer id,
                                  @ModelAttribute Aviso aviso,
                                  @RequestParam("archivoImagen") MultipartFile archivoImagen,
                                  @RequestParam("fotoAvisoUrl") String fotoAvisoUrl) {

        Optional<Aviso> optionalAviso = avisoRepository.findById(id);
        if (optionalAviso.isEmpty()) {
            return "redirect:/superadmin/avisos?error=Aviso no encontrado";
        }

        Aviso avisoExistente = optionalAviso.get();
        avisoExistente.setTituloAviso(aviso.getTituloAviso());
        avisoExistente.setTextoAviso(aviso.getTextoAviso());

        try {
            if (archivoImagen != null && !archivoImagen.isEmpty()) {
                String resultadoSubida = s3Service.uploadFile(archivoImagen);

                // ✅ Extraer la URL del mensaje usando substring
                int indexUrl = resultadoSubida.indexOf("URL:");
                if (indexUrl != -1) {
                    String urlExtraida = resultadoSubida.substring(indexUrl + 4).trim();
                    avisoExistente.setFotoAvisoUrl(urlExtraida);
                }
            } else if (fotoAvisoUrl != null && !fotoAvisoUrl.isBlank()) {
                // ✅ Usar URL ingresada manualmente
                avisoExistente.setFotoAvisoUrl(fotoAvisoUrl);
            }
            // Si no se sube nada ni se proporciona URL, se conserva la imagen actual
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/superadmin/avisos?error=Error al subir la imagen";
        }

        avisoRepository.save(avisoExistente);
        return "redirect:/superadmin/avisos?exito=Aviso actualizado correctamente";
    }



    @GetMapping("/avisos")
    public String listarAvisos(Model model) {
        // Obtener todos los avisos ordenados por fecha descendente
        List<Aviso> avisos = avisoRepository.findAll(Sort.by(Sort.Direction.DESC, "fechaAviso"));

        // Obtener el último aviso (el más reciente)
        Aviso ultimoAviso = avisos.isEmpty() ? null : avisos.get(0);

        // Agregar atributos al modelo
        model.addAttribute("avisos", avisos);
        model.addAttribute("ultimoAviso", ultimoAviso);

        return "Superadmin/verAvisos";
    }

    @GetMapping("/crearAviso")
    public String mostrarFormularioCreacion(Model model) {
        model.addAttribute("aviso", new Aviso());
        model.addAttribute("modo", "crear");
        return "Superadmin/crearAviso"; // Mismo formulario que para editar
    }

    @PostMapping("/guardarAviso")
    public String guardarAviso(
            @ModelAttribute("aviso") @Valid Aviso nuevoAviso,
            BindingResult result,
            @RequestParam("archivoImagen") MultipartFile archivoImagen,
            @RequestParam("fotoAvisoUrl") String fotoAvisoUrl,
            RedirectAttributes redirectAttributes
    ) {
        if (result.hasErrors()) {
            return "Superadmin/crearAviso";
        }

        // Establecer fecha actual si no viene definida
        if (nuevoAviso.getFechaAviso() == null) {
            nuevoAviso.setFechaAviso(LocalDateTime.now());
        }

        // Establecer estado por defecto si no viene definido
        if (nuevoAviso.getEstadoAviso() == null || nuevoAviso.getEstadoAviso().isEmpty()) {
            nuevoAviso.setEstadoAviso("disponible");
        }

        try {
            if (archivoImagen != null && !archivoImagen.isEmpty()) {
                String resultado = s3Service.uploadFile(archivoImagen);
                int indexUrl = resultado.indexOf("URL:");
                if (indexUrl != -1) {
                    String url = resultado.substring(indexUrl + 4).trim();
                    nuevoAviso.setFotoAvisoUrl(url);
                }
            } else if (fotoAvisoUrl != null && !fotoAvisoUrl.isBlank()) {
                nuevoAviso.setFotoAvisoUrl(fotoAvisoUrl);
            } else {
                // ✅ Si no se subió archivo ni se escribió URL, usar imagen por defecto
                nuevoAviso.setFotoAvisoUrl("https://telelink-images.s3.us-east-1.amazonaws.com/BienvenidaSanMiguel.png");
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al subir la imagen");
            return "redirect:/superadmin/avisos";
        }

        avisoRepository.save(nuevoAviso);
        redirectAttributes.addFlashAttribute("exito", "Aviso creado correctamente");
        return "redirect:/superadmin/avisos";
    }

    @PostMapping("/avisos/activar/{id}")
    public String activarAviso(@PathVariable Integer id, RedirectAttributes attrs) {
        try {
            avisoService.activarAviso(id);
            attrs.addFlashAttribute("success", "Aviso activado correctamente");
        } catch (Exception e) {
            attrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/superadmin/avisos";
    }

    @PostMapping("/avisos/desactivar/{id}")
    public String desactivarAviso(@PathVariable Integer id, RedirectAttributes attrs) {
        try {
            avisoService.desactivarAviso(id);
            attrs.addFlashAttribute("success", "Aviso desactivado correctamente");
        } catch (Exception e) {
            attrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/superadmin/avisos";
    }

    @GetMapping("/reservas")
    public String listarReservas(Model model) {
        List<Reserva> reservas = reservaRepository.findAll();
        model.addAttribute("reservas", reservas);
        return "Superadmin/Reservas"; // Ruta de tu plantilla Thymeleaf
    }

    @GetMapping("reservas/{id}")
    public String verDetalleReserva(@PathVariable Integer id, Model model) {
        Reserva reserva = reservaRepository.findByReservaId(id);
        if (reserva == null) {
            // Manejar el caso cuando la reserva no existe
            return "redirect:/superadmin/reservas";
        }
        model.addAttribute("reserva", reserva);
        return "Superadmin/VerReserva"; // Plantilla para ver detalles
    }



    @GetMapping("/usuarios")
    public String listarUsuarios(Model model) {
        // Obtener estadísticas directamente desde el repositorio
        long totalUsuarios = usuarioRepository.count();
        long usuariosBaneados = usuarioRepository.countByEstadoCuenta(Usuario.EstadoCuenta.baneado);
        long nuevosUsuariosEsteMes = usuarioRepository.countUsuariosEsteMes();

        // Agregar datos al modelo
        model.addAttribute("totalUsuarios", totalUsuarios);
        model.addAttribute("usuariosBaneados", usuariosBaneados);
        model.addAttribute("nuevosUsuariosEsteMes", nuevosUsuariosEsteMes);

        // Obtener lista completa de usuarios
        List<Usuario> usuarios = usuarioRepository.findAll();
        model.addAttribute("usuarios", usuarios);

        return "Superadmin/Usuarios";
    }

    @GetMapping("usuarios/{id}")
    public String verDetalleUsuario(@PathVariable Integer id, Model model) {
        // Obtener el usuario por ID
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) {
            return "redirect:/superadmin/usuarios";
        }

        long reservasTotales = reservaRepository.countByUsuario(usuario);
        long reservasEsteMes = reservaRepository.countByUsuarioThisMonth(usuario);
        long reservasEstaSemana = reservaRepository.countByUsuarioThisWeek(usuario);

        // Obtener las últimas 6 reservas del usuario
        List<Reserva> ultimasReservas = reservaRepository.findTop6ByUsuarioOrderByInicioReservaDesc(usuario);

        // Agregar datos al modelo
        model.addAttribute("usuario", usuario);
        model.addAttribute("ultimasReservas", ultimasReservas);
        model.addAttribute("reservasTotales", reservasTotales);
        model.addAttribute("reservasEsteMes", reservasEsteMes);
        model.addAttribute("reservasEstaSemana", reservasEstaSemana);

        return "Superadmin/verPerfil";
    }

    @GetMapping("usuarios/editar/{usuarioId}")
    public String mostrarEdicionUsuario(@PathVariable("usuarioId") Integer usuarioId, Model model) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioId));
        List<Rol> roles = rolRepository.findAll();
        model.addAttribute("roles", roles);
        model.addAttribute("usuario", usuario);

        long reservasTotales = reservaRepository.countByUsuario(usuario);

        model.addAttribute("reservas", reservasTotales);

        return "Superadmin/editarPerfil";
    }

    @PostMapping("usuarios/actualizar/{usuarioId}")
    public String actualizarUsuario(
            @PathVariable Integer usuarioId,
            @ModelAttribute("usuario") @Valid Usuario usuario,
            BindingResult result,
            @RequestParam(value = "fotoPerfil", required = false) MultipartFile fotoPerfil,
            RedirectAttributes redirectAttributes,
            Model model) {

        // 1. Obtener usuario existente
        Usuario usuarioExistente = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // 2. Validación de unicidad del correo electrónico
        if (!usuarioExistente.getCorreoElectronico().equals(usuario.getCorreoElectronico())) {
            boolean correoEnUso = usuarioRepository.existsByCorreoElectronicoAndUsuarioIdNot(
                    usuario.getCorreoElectronico(),
                    usuarioId);

            if (correoEnUso) {
                result.rejectValue("correoElectronico", "duplicate.email",
                        "El correo electrónico ya está registrado");
            }
        }

        // 3. Validación de unicidad del teléfono (si el campo no es nulo)
        if (usuario.getTelefono() != null && !usuario.getTelefono().isEmpty()) {
            if (!usuarioExistente.getTelefono().equals(usuario.getTelefono())) {
                boolean telefonoEnUso = usuarioRepository.existsByTelefonoAndUsuarioIdNot(
                        usuario.getTelefono(),
                        usuarioId);

                if (telefonoEnUso) {
                    result.rejectValue("telefono", "duplicate.phone",
                            "El número de teléfono ya está registrado");
                }
            }
        }

        // 4. Validación del archivo de imagen
        if (fotoPerfil != null && !fotoPerfil.isEmpty()) {
            // Validar tamaño (2MB máximo)
            if (fotoPerfil.getSize() > 2097152) {
                result.rejectValue("fotoPerfilUrl", "size.exceeded",
                        "El tamaño de la imagen no debe exceder 2MB");
            }

            // Validar tipo de archivo
            String contentType = fotoPerfil.getContentType();
            if (contentType == null ||
                    (!contentType.equalsIgnoreCase("image/jpeg") &&
                            !contentType.equalsIgnoreCase("image/png"))) {
                result.rejectValue("fotoPerfilUrl", "invalid.type",
                        "Solo se aceptan imágenes en formato JPG o PNG");
            }
        }

        // 5. Si hay errores, recargar el formulario
        if (result.hasErrors()) {
            model.addAttribute("roles", rolRepository.findAll());
            return "Superadmin/editarPerfil/" + usuarioId;
        }

        if (fotoPerfil != null && !fotoPerfil.isEmpty()) {
            // Eliminar la imagen anterior
            if (usuarioExistente.getFotoPerfilUrl() != null) {
                String keyAnterior = usuarioExistente.getFotoPerfilUrl();
                s3Service.deleteFile(keyAnterior);
            }

            // Subir la nueva imagen
            String uploadResult = s3Service.uploadFile(fotoPerfil);
            String fotoPerfilUrl = uploadResult.substring(uploadResult.indexOf("URL: ") + 5).trim();

            usuario.setFotoPerfilUrl(fotoPerfilUrl);
        } else {
            usuario.setFotoPerfilUrl(usuarioExistente.getFotoPerfilUrl());
        }

        // Campos protegidos
        usuario.setUsuarioId(usuarioId);
        usuario.setContraseniaHash(usuarioExistente.getContraseniaHash());
        usuario.setFechaCreacion(usuarioExistente.getFechaCreacion());
        usuario.setFechaActualizacion(LocalDateTime.now());
        usuario.setApellidos(usuarioExistente.getApellidos());
        usuario.setNombres(usuarioExistente.getNombres());
        usuario.setEstadoCuenta(usuarioExistente.getEstadoCuenta());
        usuario.setDni(usuarioExistente.getDni());

        // Rol
        if (usuario.getRol() != null && usuario.getRol().getRolId() != null) {
            Rol rolCompleto = rolRepository.findById(usuario.getRol().getRolId())
                    .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado"));
            usuario.setRol(rolCompleto);
        }

        usuarioRepository.save(usuario);
        redirectAttributes.addFlashAttribute("successMessage", "Perfil actualizado exitosamente");
        return "redirect:/superadmin/usuarios/" + usuarioId;

    }

    @PostMapping("/usuarios/{id}/banear")
    public String banearUsuario(@PathVariable("id") Integer usuarioId,
                                @RequestParam(required = false) String motivo,
                                RedirectAttributes redirectAttrs) {

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 1. Cambiar estado del usuario
        usuario.setEstadoCuenta(Usuario.EstadoCuenta.baneado);
        usuario.setFechaActualizacion(LocalDateTime.now());
        usuarioRepository.save(usuario);

        ActividadUsuario actividadUsuario = new ActividadUsuario();
        actividadUsuario.setUsuario(usuario);
        actividadUsuario.setDetalles("Baneo de usuario");


        TipoActividad tipoBaneo = tipoActividadRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("Tipo de actividad no encontrado"));

        actividadUsuario.setTipoActividad(tipoBaneo);
        actividadUsuarioRepository.save(actividadUsuario);

        // 2. Cancelar reservas confirmadas
        List<Reserva> reservas = reservaRepository.findByUsuarioAndEstado(usuario, Reserva.Estado.confirmada);
        for (Reserva reserva : reservas) {
            reserva.setEstado(Reserva.Estado.cancelada);
            reserva.setRazonCancelacion("Usuario baneado" + (motivo != null ? ": " + motivo : ""));
            reserva.setFechaActualizacion(LocalDateTime.now());
            reservaRepository.save(reserva);

            // 3. Crear reembolsos para pagos asociados
            pagoRepository.findByReserva_ReservaId(reserva.getReservaId()).ifPresent(pago -> {
                Reembolso reembolso = new Reembolso();
                reembolso.setMonto(pago.getMonto());
                reembolso.setMotivo("Reembolso por baneo de usuario");
                reembolso.setFechaReembolso(LocalDateTime.now());
                reembolso.setPago(pago);
                reembolsoRepository.save(reembolso);
            });
        }

        redirectAttrs.addFlashAttribute("success", "Usuario baneado exitosamente");
        return "redirect:/superadmin/usuarios";
    }

    @PostMapping("usuarios/{id}/activar")
    public String activarUsuario(@PathVariable("id") Integer usuarioId,
                                 RedirectAttributes redirectAttrs) {

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Solo cambiar estado sin modificar reservas
        usuario.setEstadoCuenta(Usuario.EstadoCuenta.activo);
        usuario.setFechaActualizacion(LocalDateTime.now());
        usuarioRepository.save(usuario);

        redirectAttrs.addFlashAttribute("success", "Usuario activado exitosamente");
        return "redirect:/superadmin/usuarios";
    }
    @GetMapping("/pagos")
    public String listarPagos(Model model) {
        List<Pago> pagos = pagoRepository.findAll();
        model.addAttribute("pagos", pagos);
        return "Superadmin/verPagos";
    }

    @GetMapping("/reservas/detalles/{id}")
    public String getDetalleReserva(@PathVariable("id") Integer reservaId, Model model) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        model.addAttribute("reserva", reserva);
        return "Superadmin/fragments/detalle-reserva :: detalleFragment";
    }

    @GetMapping("/pagos/detalles/{id}")
    public String getDetallePago(@PathVariable("id") Integer pagoId, Model model) {
        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));

        model.addAttribute("pago", pago);
        return "Superadmin/fragments/detalle-pago :: detallePagoFragment";
    }

    @GetMapping("/reembolsos/detalles/{id}")
    public String getDetalleReembolso(@PathVariable("id") Integer reembolsoId, Model model) {
        Reembolso reembolso = reembolsoRepository.findById(reembolsoId)
                .orElseThrow(() -> new RuntimeException("Reembolso no encontrado"));

        model.addAttribute("reembolso", reembolso);
        return "Superadmin/fragments/detalle-reembolso :: detalleFragment";
    }

    @GetMapping("usuarios/{id}/detalles")
    public String verPerfilUsuario(@PathVariable Integer id, Model model) {
        // 1. Obtener datos del usuario
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (usuario == null) {
            return "redirect:/superadmin/usuarios";
        }

        long reservasTotales = reservaRepository.countByUsuario(usuario);
        long reservasEsteMes = reservaRepository.countByUsuarioThisMonth(usuario);
        long reservasEstaSemana = reservaRepository.countByUsuarioThisWeek(usuario);

        // 2. Obtener todas las relaciones
        List<Reserva> reservas = reservaRepository.findByUsuario_UsuarioId(id);
        List<Pago> pagos = pagoRepository.findByReserva_Usuario_UsuarioId(id);
        List<Reembolso> reembolsos = reembolsoRepository.findByPago_Reserva_Usuario_UsuarioId(id);

        // 3. Agregar al modelo
        model.addAttribute("usuario", usuario);
        model.addAttribute("reservas", reservas);
        model.addAttribute("pagos", pagos);
        model.addAttribute("reembolsos", reembolsos);
        model.addAttribute("reservasTotales", reservasTotales);
        model.addAttribute("reservasEsteMes", reservasEsteMes);
        model.addAttribute("reservasEstaSemana", reservasEstaSemana);

        return "Superadmin/verPerfilDetallado"; // Nombre de tu archivo HTML
    }

    @GetMapping("/usuariosAdmin")
    public String gestionUsuarios(Model model) {
        // Obtener administradores (rol_id = 1 por ejemplo)
        List<Usuario> administradores = usuarioRepository.findByRolId(2);

        // Obtener coordinadores (rol_id = 2 por ejemplo)
        List<Usuario> coordinadores = usuarioRepository.findByRolId(4);

        model.addAttribute("administradores", administradores);
        model.addAttribute("coordinadores", coordinadores);

        return "Superadmin/verAdminCoord";
    }


    @GetMapping("/transacciones")
    public String listarTransacciones() {
        return "Superadmin/Transacciones";
    }

    @GetMapping("/verPagosReservas")
    public String listarPagosReservasPerfil() {
        return "verPerfilDetallado";
    }

}
