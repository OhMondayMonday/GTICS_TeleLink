package com.example.telelink.controller;

import com.example.telelink.entity.*;

import com.example.telelink.repository.*;
import com.example.telelink.service.AvisoService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.telelink.service.S3Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
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
import java.time.format.DateTimeFormatter;

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
        BigDecimal pagoOnlineMensual = pagoRepository.obtenerMontoMensualPorMetodoPago(1); // ID para Pago Online
        BigDecimal pagoDepositoMensual = pagoRepository.obtenerMontoMensualPorMetodoPago(2); // ID para Depósito Bancario
        BigDecimal pagoOnlineSemanal = pagoRepository.obtenerMontoSemanalPorMetodoPago(1); // ID para Pago Online
        BigDecimal pagoDepositoSemanal = pagoRepository.obtenerMontoSemanalPorMetodoPago(2); // ID para Depósito Bancario
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
        model.addAttribute("pagoOnlineMensual", pagoOnlineMensual);
        model.addAttribute("pagoDepositoMensual", pagoDepositoMensual);
        model.addAttribute("pagoOnlineSemanal", pagoOnlineSemanal);
        model.addAttribute("pagoDepositoSemanal", pagoDepositoSemanal);

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
        List<Usuario> usuarios = usuarioRepository.findAllExceptSuperadmin();
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

    @GetMapping("/usuarios/editar/{usuarioId}")
    public String mostrarFormularioEdicionPerfil(@PathVariable Integer usuarioId, Model model) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        model.addAttribute("usuario", usuario);       // Para mostrar perfil
        model.addAttribute("usuarioForm", usuario);
        long  reservasTotales = reservaRepository.countByUsuario(usuario);// Para editar campos
        model.addAttribute("roles", rolRepository.findAll());
        model.addAttribute("reservasTotales", reservasTotales);

        return "Superadmin/editarPerfil";
    }

    @PostMapping("/usuarios/actualizar/{usuarioId}")
    public String actualizarUsuario(
            @PathVariable Integer usuarioId,
            @ModelAttribute("usuarioForm") @Valid Usuario usuarioForm,
            BindingResult result,
            @RequestParam(value = "fotoPerfil", required = false) MultipartFile fotoPerfil,
            RedirectAttributes redirectAttributes,
            Model model) {

        Usuario usuarioExistente = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Validación de correo electrónico único
        if (!usuarioForm.getCorreoElectronico().equals(usuarioExistente.getCorreoElectronico()) &&
                usuarioRepository.existsByCorreoElectronicoAndUsuarioIdNot(usuarioForm.getCorreoElectronico(), usuarioId)) {
            result.rejectValue("correoElectronico", "duplicate.email", "El correo electrónico ya está registrado");
        }

        // Validación de número de teléfono único
        if (usuarioForm.getTelefono() != null &&
                !usuarioForm.getTelefono().equals(usuarioExistente.getTelefono()) &&
                usuarioRepository.existsByTelefonoAndUsuarioIdNot(usuarioForm.getTelefono(), usuarioId)) {
            result.rejectValue("telefono", "duplicate.phone", "El número de teléfono ya está registrado");
        }

        // Validación del archivo de imagen
        if (fotoPerfil != null && !fotoPerfil.isEmpty()) {

            // Tamaño máximo: 5MB
            if (fotoPerfil.getSize() > 5 * 1024 * 1024) {
                result.rejectValue("fotoPerfilUrl", "size.exceeded", "La imagen no debe exceder los 5MB");
            }

            String originalFilename = fotoPerfil.getOriginalFilename();
            String contentType = fotoPerfil.getContentType();

            // Validación de tipo MIME
            if (contentType == null ||
                    (!contentType.equalsIgnoreCase("image/jpeg") &&
                            !contentType.equalsIgnoreCase("image/png"))) {
                result.rejectValue("fotoPerfilUrl", "invalid.type", "Solo se aceptan imágenes JPG o PNG");
            }

            // Validación de extensión
            if (originalFilename == null ||
                    (!originalFilename.toLowerCase().endsWith(".jpg") &&
                            !originalFilename.toLowerCase().endsWith(".jpeg") &&
                            !originalFilename.toLowerCase().endsWith(".png"))) {
                result.rejectValue("fotoPerfilUrl", "invalid.extension", "El archivo debe tener extensión .jpg, .jpeg o .png");
            }
        }

        if (result.hasErrors()) {
            long  reservasTotales = reservaRepository.countByUsuario(usuarioExistente);
            model.addAttribute("usuario", usuarioExistente);
            model.addAttribute("reservasTotales", reservasTotales);
            return "Superadmin/editarPerfil";
        }

        // Actualizar campos editables
        usuarioExistente.setCorreoElectronico(usuarioForm.getCorreoElectronico());
        usuarioExistente.setTelefono(usuarioForm.getTelefono());
        usuarioExistente.setDireccion(usuarioForm.getDireccion());
        usuarioExistente.setFechaActualizacion(LocalDateTime.now());

        // Manejo de foto
        if (fotoPerfil != null && !fotoPerfil.isEmpty()) {
            if (usuarioExistente.getFotoPerfilUrl() != null) {
                s3Service.deleteFile(usuarioExistente.getFotoPerfilUrl());
            }
            String uploadResult = s3Service.uploadFile(fotoPerfil);
            String fotoPerfilUrl = uploadResult.substring(uploadResult.indexOf("URL: ") + 5).trim();
            usuarioExistente.setFotoPerfilUrl(fotoPerfilUrl);
        }

        usuarioRepository.save(usuarioExistente);
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
    @GetMapping("/usuarios/export/excel")
public ResponseEntity<byte[]> exportarUsuariosExcel() {
    try {
        List<Usuario> usuarios = usuarioRepository.findAllExceptSuperadmin();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Usuarios");

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
        String[] headers = {"ID", "Nombres", "Apellidos", "Correo Electrónico", "DNI", "Teléfono", "Rol", "Estado Cuenta", "Fecha Creación"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Llenar datos
        int rowNum = 1;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        for (Usuario usuario : usuarios) {
            Row row = sheet.createRow(rowNum++);

            Cell idCell = row.createCell(0);
            idCell.setCellValue(usuario.getUsuarioId());
            idCell.setCellStyle(dataStyle);

            Cell nombresCell = row.createCell(1);
            nombresCell.setCellValue(usuario.getNombres() != null ? usuario.getNombres() : "");
            nombresCell.setCellStyle(dataStyle);

            Cell apellidosCell = row.createCell(2);
            apellidosCell.setCellValue(usuario.getApellidos() != null ? usuario.getApellidos() : "");
            apellidosCell.setCellStyle(dataStyle);

            Cell correoCell = row.createCell(3);
            correoCell.setCellValue(usuario.getCorreoElectronico() != null ? usuario.getCorreoElectronico() : "");
            correoCell.setCellStyle(dataStyle);

            Cell dniCell = row.createCell(4);
            dniCell.setCellValue(usuario.getDni() != null ? usuario.getDni() : "");
            dniCell.setCellStyle(dataStyle);

            Cell telefonoCell = row.createCell(5);
            telefonoCell.setCellValue(usuario.getTelefono() != null ? usuario.getTelefono() : "");
            telefonoCell.setCellStyle(dataStyle);

            Cell rolCell = row.createCell(6);
            rolCell.setCellValue(usuario.getRol() != null ? usuario.getRol().getRol() : "");
            rolCell.setCellStyle(dataStyle);

            Cell estadoCell = row.createCell(7);
            estadoCell.setCellValue(usuario.getEstadoCuenta() != null ? usuario.getEstadoCuenta().toString() : "");
            estadoCell.setCellStyle(dataStyle);

            Cell fechaCell = row.createCell(8);
            fechaCell.setCellValue(usuario.getFechaCreacion() != null ? usuario.getFechaCreacion().format(formatter) : "");
            fechaCell.setCellStyle(dataStyle);
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
        responseHeaders.setContentDispositionFormData("attachment", "usuarios_superadmin.xlsx");

        return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(outputStream.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/usuarios/export/pdf")
public ResponseEntity<byte[]> exportarUsuariosPdf() {
    try {
        List<Usuario> usuarios = usuarioRepository.findAllExceptSuperadmin();

        Document document = new Document(PageSize.A4.rotate()); // Orientación horizontal
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, outputStream);

        document.open();

        // Título
        com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
        Paragraph title = new Paragraph("Reporte de Usuarios - Superadmin", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Fecha de generación
        com.itextpdf.text.Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
        Paragraph dateGenerated = new Paragraph("Fecha de generación: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                dateFont);
        dateGenerated.setAlignment(Element.ALIGN_CENTER);
        dateGenerated.setSpacingAfter(20);
        document.add(dateGenerated);

        // Crear tabla
        PdfPTable table = new PdfPTable(9);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        // Configurar anchos de columnas
        float[] columnWidths = {0.5f, 1.5f, 1.5f, 2f, 1f, 1f, 1f, 1f, 1.5f};
        table.setWidths(columnWidths);

        // Estilo para encabezados
        com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.WHITE);
        BaseColor headerColor = new BaseColor(52, 58, 64);

        // Agregar encabezados
        String[] headers = {"ID", "Nombres", "Apellidos", "Correo Electrónico", "DNI", "Teléfono", "Rol", "Estado", "Fecha Creación"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(headerColor);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(6);
            table.addCell(cell);
        }

        // Estilo para datos
        com.itextpdf.text.Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Agregar datos
        for (Usuario usuario : usuarios) {
            // ID
            PdfPCell idCell = new PdfPCell(new Phrase(String.valueOf(usuario.getUsuarioId()), dataFont));
            idCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            idCell.setPadding(4);
            table.addCell(idCell);

            // Nombres
            PdfPCell nombresCell = new PdfPCell(new Phrase(usuario.getNombres() != null ? usuario.getNombres() : "", dataFont));
            nombresCell.setPadding(4);
            table.addCell(nombresCell);

            // Apellidos
            PdfPCell apellidosCell = new PdfPCell(new Phrase(usuario.getApellidos() != null ? usuario.getApellidos() : "", dataFont));
            apellidosCell.setPadding(4);
            table.addCell(apellidosCell);

            // Correo
            PdfPCell correoCell = new PdfPCell(new Phrase(usuario.getCorreoElectronico() != null ? usuario.getCorreoElectronico() : "", dataFont));
            correoCell.setPadding(4);
            table.addCell(correoCell);

            // DNI
            PdfPCell dniCell = new PdfPCell(new Phrase(usuario.getDni() != null ? usuario.getDni() : "", dataFont));
            dniCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            dniCell.setPadding(4);
            table.addCell(dniCell);

            // Teléfono
            PdfPCell telefonoCell = new PdfPCell(new Phrase(usuario.getTelefono() != null ? usuario.getTelefono() : "", dataFont));
            telefonoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            telefonoCell.setPadding(4);
            table.addCell(telefonoCell);

            // Rol
            PdfPCell rolCell = new PdfPCell(new Phrase(usuario.getRol() != null ? usuario.getRol().getRol() : "", dataFont));
            rolCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            rolCell.setPadding(4);
            table.addCell(rolCell);

            // Estado
            PdfPCell estadoCell = new PdfPCell(new Phrase(usuario.getEstadoCuenta() != null ? usuario.getEstadoCuenta().toString() : "", dataFont));
            estadoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            estadoCell.setPadding(4);
            table.addCell(estadoCell);

            // Fecha
            PdfPCell fechaCell = new PdfPCell(new Phrase(usuario.getFechaCreacion() != null ? usuario.getFechaCreacion().format(formatter) : "", dataFont));
            fechaCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            fechaCell.setPadding(4);
            table.addCell(fechaCell);
        }

        document.add(table);
        document.close();

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_PDF);
        responseHeaders.setContentDispositionFormData("attachment", "usuarios_superadmin.pdf");

        return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(outputStream.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== EXPORTACIÓN ADMINISTRADORES ====================
@GetMapping("/administradores/export/excel")
public ResponseEntity<byte[]> exportarAdministradoresExcel() {
    try {
        List<Usuario> administradores = usuarioRepository.findByRolId(2);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Administradores");

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
        String[] headers = {"ID", "Nombres", "Apellidos", "Correo Electrónico", "DNI", "Teléfono", "Estado Cuenta", "Fecha Creación"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Llenar datos
        int rowNum = 1;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        for (Usuario admin : administradores) {
            Row row = sheet.createRow(rowNum++);

            Cell idCell = row.createCell(0);
            idCell.setCellValue(admin.getUsuarioId());
            idCell.setCellStyle(dataStyle);

            Cell nombresCell = row.createCell(1);
            nombresCell.setCellValue(admin.getNombres() != null ? admin.getNombres() : "");
            nombresCell.setCellStyle(dataStyle);

            Cell apellidosCell = row.createCell(2);
            apellidosCell.setCellValue(admin.getApellidos() != null ? admin.getApellidos() : "");
            apellidosCell.setCellStyle(dataStyle);

            Cell correoCell = row.createCell(3);
            correoCell.setCellValue(admin.getCorreoElectronico() != null ? admin.getCorreoElectronico() : "");
            correoCell.setCellStyle(dataStyle);

            Cell dniCell = row.createCell(4);
            dniCell.setCellValue(admin.getDni() != null ? admin.getDni() : "");
            dniCell.setCellStyle(dataStyle);

            Cell telefonoCell = row.createCell(5);
            telefonoCell.setCellValue(admin.getTelefono() != null ? admin.getTelefono() : "");
            telefonoCell.setCellStyle(dataStyle);

            Cell estadoCell = row.createCell(6);
            estadoCell.setCellValue(admin.getEstadoCuenta() != null ? admin.getEstadoCuenta().toString() : "");
            estadoCell.setCellStyle(dataStyle);

            Cell fechaCell = row.createCell(7);
            fechaCell.setCellValue(admin.getFechaCreacion() != null ? admin.getFechaCreacion().format(formatter) : "");
            fechaCell.setCellStyle(dataStyle);
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
        responseHeaders.setContentDispositionFormData("attachment", "administradores_superadmin.xlsx");

        return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(outputStream.toByteArray());

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}

@GetMapping("/administradores/export/pdf")
public ResponseEntity<byte[]> exportarAdministradoresPdf() {
    try {
        List<Usuario> administradores = usuarioRepository.findByRolId(2);

        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, outputStream);

        document.open();

        // Título
        com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
        Paragraph title = new Paragraph("Reporte de Administradores - Superadmin", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Fecha de generación
        com.itextpdf.text.Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
        Paragraph dateGenerated = new Paragraph("Fecha de generación: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                dateFont);
        dateGenerated.setAlignment(Element.ALIGN_CENTER);
        dateGenerated.setSpacingAfter(20);
        document.add(dateGenerated);

        // Crear tabla
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        // Configurar anchos de columnas
        float[] columnWidths = {0.5f, 1.5f, 1.5f, 2f, 1f, 1f, 1f, 1.5f};
        table.setWidths(columnWidths);

        // Estilo para encabezados
        com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.WHITE);
        BaseColor headerColor = new BaseColor(52, 58, 64);

        // Agregar encabezados
        String[] headers = {"ID", "Nombres", "Apellidos", "Correo Electrónico", "DNI", "Teléfono", "Estado", "Fecha Creación"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(headerColor);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(6);
            table.addCell(cell);
        }

        // Estilo para datos
        com.itextpdf.text.Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Agregar datos
        for (Usuario admin : administradores) {
            PdfPCell idCell = new PdfPCell(new Phrase(String.valueOf(admin.getUsuarioId()), dataFont));
            idCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            idCell.setPadding(4);
            table.addCell(idCell);

            PdfPCell nombresCell = new PdfPCell(new Phrase(admin.getNombres() != null ? admin.getNombres() : "", dataFont));
            nombresCell.setPadding(4);
            table.addCell(nombresCell);

            PdfPCell apellidosCell = new PdfPCell(new Phrase(admin.getApellidos() != null ? admin.getApellidos() : "", dataFont));
            apellidosCell.setPadding(4);
            table.addCell(apellidosCell);

            PdfPCell correoCell = new PdfPCell(new Phrase(admin.getCorreoElectronico() != null ? admin.getCorreoElectronico() : "", dataFont));
            correoCell.setPadding(4);
            table.addCell(correoCell);

            PdfPCell dniCell = new PdfPCell(new Phrase(admin.getDni() != null ? admin.getDni() : "", dataFont));
            dniCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            dniCell.setPadding(4);
            table.addCell(dniCell);

            PdfPCell telefonoCell = new PdfPCell(new Phrase(admin.getTelefono() != null ? admin.getTelefono() : "", dataFont));
            telefonoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            telefonoCell.setPadding(4);
            table.addCell(telefonoCell);

            PdfPCell estadoCell = new PdfPCell(new Phrase(admin.getEstadoCuenta() != null ? admin.getEstadoCuenta().toString() : "", dataFont));
            estadoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            estadoCell.setPadding(4);
            table.addCell(estadoCell);

            PdfPCell fechaCell = new PdfPCell(new Phrase(admin.getFechaCreacion() != null ? admin.getFechaCreacion().format(formatter) : "", dataFont));
            fechaCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            fechaCell.setPadding(4);
            table.addCell(fechaCell);
        }

        document.add(table);
        document.close();

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_PDF);
        responseHeaders.setContentDispositionFormData("attachment", "administradores_superadmin.pdf");

        return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(outputStream.toByteArray());

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}

// ==================== EXPORTACIÓN COORDINADORES ====================
@GetMapping("/coordinadores/export/excel")
public ResponseEntity<byte[]> exportarCoordinadoresExcel() {
    try {
        List<Usuario> coordinadores = usuarioRepository.findByRolId(4);

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
        String[] headers = {"ID", "Nombres", "Apellidos", "Correo Electrónico", "DNI", "Teléfono", "Estado Cuenta", "Fecha Creación"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Llenar datos
        int rowNum = 1;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        for (Usuario coord : coordinadores) {
            Row row = sheet.createRow(rowNum++);

            Cell idCell = row.createCell(0);
            idCell.setCellValue(coord.getUsuarioId());
            idCell.setCellStyle(dataStyle);

            Cell nombresCell = row.createCell(1);
            nombresCell.setCellValue(coord.getNombres() != null ? coord.getNombres() : "");
            nombresCell.setCellStyle(dataStyle);

            Cell apellidosCell = row.createCell(2);
            apellidosCell.setCellValue(coord.getApellidos() != null ? coord.getApellidos() : "");
            apellidosCell.setCellStyle(dataStyle);

            Cell correoCell = row.createCell(3);
            correoCell.setCellValue(coord.getCorreoElectronico() != null ? coord.getCorreoElectronico() : "");
            correoCell.setCellStyle(dataStyle);

            Cell dniCell = row.createCell(4);
            dniCell.setCellValue(coord.getDni() != null ? coord.getDni() : "");
            dniCell.setCellStyle(dataStyle);

            Cell telefonoCell = row.createCell(5);
            telefonoCell.setCellValue(coord.getTelefono() != null ? coord.getTelefono() : "");
            telefonoCell.setCellStyle(dataStyle);

            Cell estadoCell = row.createCell(6);
            estadoCell.setCellValue(coord.getEstadoCuenta() != null ? coord.getEstadoCuenta().toString() : "");
            estadoCell.setCellStyle(dataStyle);

            Cell fechaCell = row.createCell(7);
            fechaCell.setCellValue(coord.getFechaCreacion() != null ? coord.getFechaCreacion().format(formatter) : "");
            fechaCell.setCellStyle(dataStyle);
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
        responseHeaders.setContentDispositionFormData("attachment", "coordinadores_superadmin.xlsx");

        return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(outputStream.toByteArray());

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}

@GetMapping("/coordinadores/export/pdf")
public ResponseEntity<byte[]> exportarCoordinadoresPdf() {
    try {
        List<Usuario> coordinadores = usuarioRepository.findByRolId(4);

        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, outputStream);

        document.open();

        // Título
        com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
        Paragraph title = new Paragraph("Reporte de Coordinadores - Superadmin", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Fecha de generación
        com.itextpdf.text.Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
        Paragraph dateGenerated = new Paragraph("Fecha de generación: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                dateFont);
        dateGenerated.setAlignment(Element.ALIGN_CENTER);
        dateGenerated.setSpacingAfter(20);
        document.add(dateGenerated);

        // Crear tabla
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        // Configurar anchos de columnas
        float[] columnWidths = {0.5f, 1.5f, 1.5f, 2f, 1f, 1f, 1f, 1.5f};
        table.setWidths(columnWidths);

        // Estilo para encabezados
        com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.WHITE);
        BaseColor headerColor = new BaseColor(52, 58, 64);

        // Agregar encabezados
        String[] headers = {"ID", "Nombres", "Apellidos", "Correo Electrónico", "DNI", "Teléfono", "Estado", "Fecha Creación"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(headerColor);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(6);
            table.addCell(cell);
        }

        // Estilo para datos
        com.itextpdf.text.Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Agregar datos
        for (Usuario coord : coordinadores) {
            PdfPCell idCell = new PdfPCell(new Phrase(String.valueOf(coord.getUsuarioId()), dataFont));
            idCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            idCell.setPadding(4);
            table.addCell(idCell);

            PdfPCell nombresCell = new PdfPCell(new Phrase(coord.getNombres() != null ? coord.getNombres() : "", dataFont));
            nombresCell.setPadding(4);
            table.addCell(nombresCell);

            PdfPCell apellidosCell = new PdfPCell(new Phrase(coord.getApellidos() != null ? coord.getApellidos() : "", dataFont));
            apellidosCell.setPadding(4);
            table.addCell(apellidosCell);

            PdfPCell correoCell = new PdfPCell(new Phrase(coord.getCorreoElectronico() != null ? coord.getCorreoElectronico() : "", dataFont));
            correoCell.setPadding(4);
            table.addCell(correoCell);

            PdfPCell dniCell = new PdfPCell(new Phrase(coord.getDni() != null ? coord.getDni() : "", dataFont));
            dniCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            dniCell.setPadding(4);
            table.addCell(dniCell);

            PdfPCell telefonoCell = new PdfPCell(new Phrase(coord.getTelefono() != null ? coord.getTelefono() : "", dataFont));
            telefonoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            telefonoCell.setPadding(4);
            table.addCell(telefonoCell);

            PdfPCell estadoCell = new PdfPCell(new Phrase(coord.getEstadoCuenta() != null ? coord.getEstadoCuenta().toString() : "", dataFont));
            estadoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            estadoCell.setPadding(4);
            table.addCell(estadoCell);

            PdfPCell fechaCell = new PdfPCell(new Phrase(coord.getFechaCreacion() != null ? coord.getFechaCreacion().format(formatter) : "", dataFont));
            fechaCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            fechaCell.setPadding(4);
            table.addCell(fechaCell);
        }

        document.add(table);
        document.close();

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_PDF);
        responseHeaders.setContentDispositionFormData("attachment", "coordinadores_superadmin.pdf");

        return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(outputStream.toByteArray());

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}

// ==================== EXPORTACIÓN PAGOS ====================
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

        // Crear encabezados
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Usuario", "Reserva ID", "Método de Pago", "Monto", "Fecha Pago", "Estado"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Llenar datos
        int rowNum = 1;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        for (Pago pago : pagos) {
            Row row = sheet.createRow(rowNum++);

            Cell idCell = row.createCell(0);
            idCell.setCellValue(pago.getPagoId());
            idCell.setCellStyle(dataStyle);

            Cell usuarioCell = row.createCell(1);
            usuarioCell.setCellValue(pago.getReserva() != null && pago.getReserva().getUsuario() != null ? 
                pago.getReserva().getUsuario().getNombres() + " " + pago.getReserva().getUsuario().getApellidos() : "");
            usuarioCell.setCellStyle(dataStyle);

            Cell reservaCell = row.createCell(2);
            reservaCell.setCellValue(pago.getReserva() != null ? pago.getReserva().getReservaId() : 0);
            reservaCell.setCellStyle(dataStyle);

            Cell metodoCell = row.createCell(3);
            metodoCell.setCellValue(pago.getMetodoPago() != null ? pago.getMetodoPago().getMetodoPago() : "");
            metodoCell.setCellStyle(dataStyle);

            Cell montoCell = row.createCell(4);
            montoCell.setCellValue(pago.getMonto() != null ? pago.getMonto().doubleValue() : 0.0);
            montoCell.setCellStyle(dataStyle);

            Cell fechaCell = row.createCell(5);
            fechaCell.setCellValue(pago.getFechaPago() != null ? pago.getFechaPago().format(formatter) : "");
            fechaCell.setCellStyle(dataStyle);

            Cell estadoCell = row.createCell(6);
            estadoCell.setCellValue(pago.getEstadoTransaccion() != null ? pago.getEstadoTransaccion().toString() : "");
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
        responseHeaders.setContentDispositionFormData("attachment", "pagos_superadmin.xlsx");

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
        Paragraph title = new Paragraph("Reporte de Pagos - Superadmin", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Fecha de generación
        com.itextpdf.text.Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
        Paragraph dateGenerated = new Paragraph("Fecha de generación: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                dateFont);
        dateGenerated.setAlignment(Element.ALIGN_CENTER);
        dateGenerated.setSpacingAfter(20);
        document.add(dateGenerated);

        // Crear tabla
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        // Configurar anchos de columnas
        float[] columnWidths = {0.5f, 2f, 1f, 1.5f, 1f, 1.5f, 1f};
        table.setWidths(columnWidths);

        // Estilo para encabezados
        com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.WHITE);
        BaseColor headerColor = new BaseColor(52, 58, 64);

        // Agregar encabezados
        String[] headers = {"ID", "Usuario", "Reserva ID", "Método de Pago", "Monto", "Fecha Pago", "Estado"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(headerColor);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(6);
            table.addCell(cell);
        }

        // Estilo para datos
        com.itextpdf.text.Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Agregar datos
        for (Pago pago : pagos) {
            PdfPCell idCell = new PdfPCell(new Phrase(String.valueOf(pago.getPagoId()), dataFont));
            idCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            idCell.setPadding(4);
            table.addCell(idCell);

            PdfPCell usuarioCell = new PdfPCell(new Phrase(pago.getReserva() != null && pago.getReserva().getUsuario() != null ? 
                pago.getReserva().getUsuario().getNombres() + " " + pago.getReserva().getUsuario().getApellidos() : "", dataFont));
            usuarioCell.setPadding(4);
            table.addCell(usuarioCell);

            PdfPCell reservaCell = new PdfPCell(new Phrase(String.valueOf(pago.getReserva() != null ? pago.getReserva().getReservaId() : 0), dataFont));
            reservaCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            reservaCell.setPadding(4);
            table.addCell(reservaCell);

            PdfPCell metodoCell = new PdfPCell(new Phrase(pago.getMetodoPago() != null ? pago.getMetodoPago().getMetodoPago() : "", dataFont));
            metodoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            metodoCell.setPadding(4);
            table.addCell(metodoCell);

            PdfPCell montoCell = new PdfPCell(new Phrase("S/ " + (pago.getMonto() != null ? pago.getMonto().toString() : "0.00"), dataFont));
            montoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            montoCell.setPadding(4);
            table.addCell(montoCell);

            PdfPCell fechaCell = new PdfPCell(new Phrase(pago.getFechaPago() != null ? pago.getFechaPago().format(formatter) : "", dataFont));
            fechaCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            fechaCell.setPadding(4);
            table.addCell(fechaCell);

            PdfPCell estadoCell = new PdfPCell(new Phrase(pago.getEstadoTransaccion() != null ? pago.getEstadoTransaccion().toString() : "", dataFont));
            estadoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            estadoCell.setPadding(4);
            table.addCell(estadoCell);
        }

        document.add(table);
        document.close();

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_PDF);
        responseHeaders.setContentDispositionFormData("attachment", "pagos_superadmin.pdf");

        return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(outputStream.toByteArray());

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}

// ==================== EXPORTACIÓN RESERVAS ====================
@GetMapping("/reservas/export/excel")
public ResponseEntity<byte[]> exportarReservasExcel() {
    try {
        List<Reserva> reservas = reservaRepository.findAll();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Reservas");

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
        String[] headers = {"ID", "Usuario", "Establecimiento", "Espacio", "Servicio", "Fecha Inicio", "Fecha Fin", "Estado"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Llenar datos
        int rowNum = 1;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        for (Reserva reserva : reservas) {
            Row row = sheet.createRow(rowNum++);

            Cell idCell = row.createCell(0);
            idCell.setCellValue(reserva.getReservaId());
            idCell.setCellStyle(dataStyle);

            Cell usuarioCell = row.createCell(1);
            usuarioCell.setCellValue(reserva.getUsuario() != null ? 
                reserva.getUsuario().getNombres() + " " + reserva.getUsuario().getApellidos() : "");
            usuarioCell.setCellStyle(dataStyle);

            Cell establecimientoCell = row.createCell(2);
            establecimientoCell.setCellValue(reserva.getEspacioDeportivo() != null && reserva.getEspacioDeportivo().getEstablecimientoDeportivo() != null ? 
                reserva.getEspacioDeportivo().getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre() : "");
            establecimientoCell.setCellStyle(dataStyle);

            Cell espacioCell = row.createCell(3);
            espacioCell.setCellValue(reserva.getEspacioDeportivo() != null ? reserva.getEspacioDeportivo().getNombre() : "");
            espacioCell.setCellStyle(dataStyle);

            Cell servicioCell = row.createCell(4);
            servicioCell.setCellValue(reserva.getEspacioDeportivo() != null && reserva.getEspacioDeportivo().getServicioDeportivo() != null ? 
                reserva.getEspacioDeportivo().getServicioDeportivo().getServicioDeportivo() : "");
            servicioCell.setCellStyle(dataStyle);

            Cell inicioCell = row.createCell(5);
            inicioCell.setCellValue(reserva.getInicioReserva() != null ? reserva.getInicioReserva().format(formatter) : "");
            inicioCell.setCellStyle(dataStyle);

            Cell finCell = row.createCell(6);
            finCell.setCellValue(reserva.getFinReserva() != null ? reserva.getFinReserva().format(formatter) : "");
            finCell.setCellStyle(dataStyle);

            Cell estadoCell = row.createCell(7);
            estadoCell.setCellValue(reserva.getEstado() != null ? reserva.getEstado().toString() : "");
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
        responseHeaders.setContentDispositionFormData("attachment", "reservas_superadmin.xlsx");

        return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(outputStream.toByteArray());

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}

@GetMapping("/reservas/export/pdf")
public ResponseEntity<byte[]> exportarReservasPdf() {
    try {
        List<Reserva> reservas = reservaRepository.findAll();

        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, outputStream);

        document.open();

        // Título
        com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
        Paragraph title = new Paragraph("Reporte de Reservas - Superadmin", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Fecha de generación
        com.itextpdf.text.Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
        Paragraph dateGenerated = new Paragraph("Fecha de generación: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                dateFont);
        dateGenerated.setAlignment(Element.ALIGN_CENTER);
        dateGenerated.setSpacingAfter(20);
        document.add(dateGenerated);

        // Crear tabla
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        // Configurar anchos de columnas
        float[] columnWidths = {0.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1f};
        table.setWidths(columnWidths);

        // Estilo para encabezados
        com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BaseColor.WHITE);
        BaseColor headerColor = new BaseColor(52, 58, 64);

        // Agregar encabezados
        String[] headers = {"ID", "Usuario", "Establecimiento", "Espacio", "Servicio", "Fecha Inicio", "Fecha Fin", "Estado"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(headerColor);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(4);
            table.addCell(cell);
        }

        // Estilo para datos
        com.itextpdf.text.Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 7);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        // Agregar datos
        for (Reserva reserva : reservas) {
            PdfPCell idCell = new PdfPCell(new Phrase(String.valueOf(reserva.getReservaId()), dataFont));
            idCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            idCell.setPadding(3);
            table.addCell(idCell);

            PdfPCell usuarioCell = new PdfPCell(new Phrase(reserva.getUsuario() != null ? 
                reserva.getUsuario().getNombres() + " " + reserva.getUsuario().getApellidos() : "", dataFont));
            usuarioCell.setPadding(3);
            table.addCell(usuarioCell);

            PdfPCell establecimientoCell = new PdfPCell(new Phrase(reserva.getEspacioDeportivo() != null && reserva.getEspacioDeportivo().getEstablecimientoDeportivo() != null ? 
                reserva.getEspacioDeportivo().getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre() : "", dataFont));
            establecimientoCell.setPadding(3);
            table.addCell(establecimientoCell);

            PdfPCell espacioCell = new PdfPCell(new Phrase(reserva.getEspacioDeportivo() != null ? reserva.getEspacioDeportivo().getNombre() : "", dataFont));
            espacioCell.setPadding(3);
            table.addCell(espacioCell);

            PdfPCell servicioCell = new PdfPCell(new Phrase(reserva.getEspacioDeportivo() != null && reserva.getEspacioDeportivo().getServicioDeportivo() != null ? 
                reserva.getEspacioDeportivo().getServicioDeportivo().getServicioDeportivo() : "", dataFont));
            servicioCell.setPadding(3);
            table.addCell(servicioCell);

            PdfPCell inicioCell = new PdfPCell(new Phrase(reserva.getInicioReserva() != null ? reserva.getInicioReserva().format(formatter) : "", dataFont));
            inicioCell.setPadding(3);
            table.addCell(inicioCell);

            PdfPCell finCell = new PdfPCell(new Phrase(reserva.getFinReserva() != null ? reserva.getFinReserva().format(formatter) : "", dataFont));
            finCell.setPadding(3);
            table.addCell(finCell);

            PdfPCell estadoCell = new PdfPCell(new Phrase(reserva.getEstado() != null ? reserva.getEstado().toString() : "", dataFont));
            estadoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            estadoCell.setPadding(3);
            table.addCell(estadoCell);
        }

        document.add(table);
        document.close();

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_PDF);
        responseHeaders.setContentDispositionFormData("attachment", "reservas_superadmin.pdf");

        return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(outputStream.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
