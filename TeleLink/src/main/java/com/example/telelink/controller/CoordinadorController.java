package com.example.telelink.controller;

import com.example.telelink.entity.*;
import com.example.telelink.repository.*;
import jakarta.servlet.http.HttpSession;
    import jakarta.servlet.http.HttpServletResponse;
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
    import com.example.telelink.service.S3Service;
    // Imports for export functionality
    import org.apache.poi.ss.usermodel.*;
    import org.apache.poi.xssf.usermodel.XSSFWorkbook;
    import com.itextpdf.text.*;
    import com.itextpdf.text.pdf.*;
    import com.itextpdf.text.Font.FontFamily;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.HttpHeaders;
    import org.springframework.http.MediaType;

    import java.security.Timestamp;
    import java.text.DecimalFormat;
    import java.time.LocalDateTime;
    import java.time.format.DateTimeFormatter;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;
    import java.util.Optional;
    import java.io.IOException;
    import java.io.ByteArrayOutputStream;


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

        @Autowired
        private NotificacionRepository notificacionRepository;
        
        @Autowired
        private TipoNotificacionRepository tipoNotificacionRepository;

        @Autowired
        private S3Service s3Service;

        @GetMapping("/notificaciones/list")
        public ResponseEntity<List<Notificacion>> getNotificaciones(
                @RequestParam("userId") Integer userId,
                @SessionAttribute("usuario") Usuario usuario) {
            // Verify the session user matches the requested userId for security
            if (!usuario.getUsuarioId().equals(userId)) {
                return ResponseEntity.badRequest().build();
            }

            // Fetch the latest 3 notifications for the user
            List<Notificacion> notificaciones = notificacionRepository
                    .findTop3ByUsuarioUsuarioIdOrderByFechaCreacionDesc(userId);
            return ResponseEntity.ok(notificaciones);
        }

        @GetMapping("/inicio")
        public String mostrarInicio(Model model, HttpSession session) {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            
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
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null) {
                throw new IllegalArgumentException("Usuario no encontrado en la sesión");
            }
            model.addAttribute("usuario", usuario);

            List<Asistencia> asistencias = asistenciaRepository.findByCoordinador_UsuarioIdAndEstadoEntradaNot(usuario.getUsuarioId(), Asistencia.EstadoEntrada.cancelada);
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
                Usuario usuario = (Usuario) session.getAttribute("usuario");
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
                Usuario usuario = (Usuario) session.getAttribute("usuario");
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
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            model.addAttribute("usuario", usuario);
            return "Coordinador/notificaciones";
        }

        @GetMapping("/perfil")
        public String mostrarPerfil(Model model, HttpSession session) {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            model.addAttribute("usuario", usuario);
            return "Coordinador/perfil";
        }

        @GetMapping("/editar-perfil")
        public String mostrarEditarPerfil(@ModelAttribute("usuario") Usuario usuarioActualizado, Model model, HttpSession session) {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            model.addAttribute("usuario", usuario);
            return "Coordinador/editarPerfil";
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
                return "Coordinador/editarPerfil";
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
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            model.addAttribute("usuario", usuario);

            List<EspacioDeportivo> espacios = espacioDeportivoRepository.findAll();
            model.addAttribute("espacios", espacios);

            // Crear un mapa con los colores
            Map<Integer, String> clasesEstado = new HashMap<>();
            for (EspacioDeportivo espacio : espacios) {
                String clase = obtenerClaseEstado(espacio.getEstadoServicio().name());
                clasesEstado.put(espacio.getEspacioDeportivoId(), clase);
            }
            model.addAttribute("clasesEstado", clasesEstado);            return "Coordinador/espaciosDeportivos";
        }

        @GetMapping("/espacios-deportivos/export/excel")
        public void exportarEspaciosExcel(HttpServletResponse response) throws IOException {
            List<EspacioDeportivo> espacios = espacioDeportivoRepository.findAll();
            
            // Configurar la respuesta HTTP
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=espacios_deportivos.xlsx");
            
            // Crear el workbook de Excel
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Espacios Deportivos");
              // Crear estilo para el encabezado
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            
            // Crear encabezados
            Row headerRow = sheet.createRow(0);
            String[] columns = {"ID", "Nombre", "Establecimiento", "Tipo de Servicio", "Estado", 
                               "Horario Apertura", "Horario Cierre", "Precio por Hora", "Descripción"};
            
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Llenar datos
            int rowNum = 1;
            for (EspacioDeportivo espacio : espacios) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(espacio.getEspacioDeportivoId());
                row.createCell(1).setCellValue(espacio.getNombre());
                row.createCell(2).setCellValue(espacio.getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre());
                row.createCell(3).setCellValue(espacio.getServicioDeportivo().getServicioDeportivo());
                row.createCell(4).setCellValue(getEstadoTexto(espacio.getEstadoServicio()));
                row.createCell(5).setCellValue(espacio.getHorarioApertura() != null ? espacio.getHorarioApertura().toString() : "");
                row.createCell(6).setCellValue(espacio.getHorarioCierre() != null ? espacio.getHorarioCierre().toString() : "");
                row.createCell(7).setCellValue(espacio.getPrecioPorHora() != null ? espacio.getPrecioPorHora().doubleValue() : 0.0);
                row.createCell(8).setCellValue(espacio.getDescripcion() != null ? espacio.getDescripcion() : "");
            }
            
            // Ajustar ancho de columnas
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Escribir al output stream
            workbook.write(response.getOutputStream());
            workbook.close();
        }

        @GetMapping("/espacios-deportivos/export/pdf")
        public void exportarEspaciosPdf(HttpServletResponse response) throws IOException, DocumentException {
            List<EspacioDeportivo> espacios = espacioDeportivoRepository.findAll();
            
            // Configurar la respuesta HTTP
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=espacios_deportivos.pdf");
            
            // Crear documento PDF
            Document document = new Document(PageSize.A4.rotate()); // Orientación horizontal
            PdfWriter.getInstance(document, response.getOutputStream());
            
            document.open();
              // Título
            com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD);
            Paragraph title = new Paragraph("Reporte de Espacios Deportivos", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            // Fecha de generación
            com.itextpdf.text.Font dateFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.NORMAL);
            Paragraph date = new Paragraph("Generado el: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), dateFont);
            date.setAlignment(Element.ALIGN_RIGHT);
            date.setSpacingAfter(20);
            document.add(date);
            
            // Crear tabla
            PdfPTable table = new PdfPTable(9);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);
            
            // Configurar anchos de columnas
            float[] columnWidths = {1f, 2f, 2f, 2f, 1.5f, 1.5f, 1.5f, 1.5f, 3f};
            table.setWidths(columnWidths);
              // Estilo para encabezados
            com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD, BaseColor.WHITE);
            
            // Agregar encabezados
            String[] headers = {"ID", "Nombre", "Establecimiento", "Tipo de Servicio", "Estado", 
                               "H. Apertura", "H. Cierre", "Precio/Hora", "Descripción"};
            
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(BaseColor.DARK_GRAY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(8);
                table.addCell(cell);
            }
            
            // Agregar datos
            com.itextpdf.text.Font cellFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8, com.itextpdf.text.Font.NORMAL);
            for (EspacioDeportivo espacio : espacios) {
                table.addCell(new PdfPCell(new Phrase(String.valueOf(espacio.getEspacioDeportivoId()), cellFont)));
                table.addCell(new PdfPCell(new Phrase(espacio.getNombre(), cellFont)));
                table.addCell(new PdfPCell(new Phrase(espacio.getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre(), cellFont)));
                table.addCell(new PdfPCell(new Phrase(espacio.getServicioDeportivo().getServicioDeportivo(), cellFont)));
                table.addCell(new PdfPCell(new Phrase(getEstadoTexto(espacio.getEstadoServicio()), cellFont)));
                table.addCell(new PdfPCell(new Phrase(espacio.getHorarioApertura() != null ? espacio.getHorarioApertura().toString() : "", cellFont)));
                table.addCell(new PdfPCell(new Phrase(espacio.getHorarioCierre() != null ? espacio.getHorarioCierre().toString() : "", cellFont)));
                table.addCell(new PdfPCell(new Phrase(espacio.getPrecioPorHora() != null ? "S/ " + espacio.getPrecioPorHora().toString() : "S/ 0.00", cellFont)));
                
                String descripcion = espacio.getDescripcion() != null ? espacio.getDescripcion() : "";
                if (descripcion.length() > 50) {
                    descripcion = descripcion.substring(0, 47) + "...";
                }
                table.addCell(new PdfPCell(new Phrase(descripcion, cellFont)));
            }
            
            document.add(table);
            document.close();
        }
        
        private String getEstadoTexto(EspacioDeportivo.EstadoServicio estado) {
            switch (estado) {
                case operativo:
                    return "Operativo";
                case mantenimiento:
                    return "En mantenimiento";
                case clausurado:
                    return "Clausurado";
                default:
                    return "Desconocido";
            }
        }


        @GetMapping("/espacioDetalle")
        public String mostrarDetalleEspacio(@RequestParam("espacioId") Integer espacioId, Model model, HttpSession session) {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            model.addAttribute("usuario", usuario);

            EspacioDeportivo espacio = espacioDeportivoRepository.findById(espacioId)
                    .orElseThrow(() -> new IllegalArgumentException("Espacio no encontrado"));
            model.addAttribute("espacio", espacio);

            List<Resenia> resenias = reseniaRepository.findByEspacioDeportivo_EspacioDeportivoId(espacioId);
            double promedioCalificacion = resenias.isEmpty() ? 0.0 : reseniaRepository.findByEspacioDeportivo_EspacioDeportivoId(espacioId)
                    .stream()
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
            Usuario usuario = (Usuario) session.getAttribute("usuario");
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
            Usuario usuario = (Usuario) session.getAttribute("usuario");
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
            Usuario usuario = (Usuario) session.getAttribute("usuario");
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
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null) {
                throw new IllegalArgumentException("Usuario no encontrado en la sesión");
            }
            model.addAttribute("usuario", usuario);

            Observacion observacion = observacionRepository.findById(observacionId)
                    .orElseThrow(() -> new IllegalArgumentException("Observación no encontrada"));
            if (!"pendiente".equals(observacion.getEstado().name())) {
                throw new IllegalArgumentException("Solo se pueden editar observaciones en estado 'pendiente'");
            }

            // Asegurarse de que el objeto se pase como "observacionForm" para que coincida con el @ModelAttribute en el POST
            model.addAttribute("observacionForm", observacion);
            return "Coordinador/observacionEditForm";
        }

        @PostMapping("/observacionGuardar")
        public String guardarObservacion(
                @Valid @ModelAttribute("observacionForm") Observacion observacionForm,
                BindingResult result,
                @RequestParam(value = "asistenciaId", required = false) Integer asistenciaId,
                @RequestParam(value = "foto", required = false) MultipartFile foto,
                HttpSession session,
                RedirectAttributes redirectAttributes,
                Model model) {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null) {
                redirectAttributes.addFlashAttribute("message", "Usuario no encontrado en la sesión");
                redirectAttributes.addFlashAttribute("messageType", "error");
                return "redirect:/coordinador/observaciones";
            }

            // Determinar si es creación o edición
            boolean isCreation = observacionForm.getObservacionId() == null;

            // Cargar el objeto original desde la base de datos si es edición
            Observacion originalObservacion = null;
            if (!isCreation) {
                originalObservacion = observacionRepository.findById(observacionForm.getObservacionId())
                        .orElseThrow(() -> new IllegalArgumentException("Observación no encontrada"));
            }

            // Asignar valores requeridos antes de la validación
            if (isCreation) {
                observacionForm.setCoordinador(usuario);
                observacionForm.setFechaCreacion(LocalDateTime.now());
                if (asistenciaId != null) {
                    Asistencia asistencia = asistenciaRepository.findById(asistenciaId)
                            .orElseThrow(() -> new IllegalArgumentException("Asistencia no encontrada"));
                    observacionForm.setEspacioDeportivo(asistencia.getEspacioDeportivo());
                }
            } else {
                // En edición, preservar las relaciones y campos no modificados
                observacionForm.setCoordinador(originalObservacion.getCoordinador());
                observacionForm.setEspacioDeportivo(originalObservacion.getEspacioDeportivo());
                observacionForm.setFechaCreacion(originalObservacion.getFechaCreacion());
                observacionForm.setFechaActualizacion(LocalDateTime.now());
                observacionForm.setEstado(originalObservacion.getEstado());
                observacionForm.setComentarioAdministrador(originalObservacion.getComentarioAdministrador());
            }

            // Validar manualmente la foto
            String defaultFotoUrl = "https://media-cdn.tripadvisor.com/media/photo-s/12/34/6a/8f/cancha-de-futbol-redes.jpg";
            String existingFotoUrl = isCreation ? null : originalObservacion.getFotoUrl();
            if (isCreation) {
                if (foto == null || foto.isEmpty()) {
                    result.rejectValue("fotoUrl", "NotBlank", "La foto es obligatoria al crear una observación");
                } else {
                    String contentType = foto.getContentType();
                    if (contentType == null || !contentType.matches("^(image/(jpeg|png|jpg))$")) {
                        result.rejectValue("fotoUrl", "typeMismatch", "El archivo debe ser una imagen (JPEG, PNG o JPG)");
                    }
                }
            } else if (foto != null && !foto.isEmpty()) {
                String contentType = foto.getContentType();
                if (contentType == null || !contentType.matches("^(image/(jpeg|png|jpg))$")) {
                    result.rejectValue("fotoUrl", "typeMismatch", "El archivo debe ser una imagen (JPEG, PNG o JPG)");
                }
            }

            // Evaluar errores después de asignar los campos requeridos
            if (result.hasErrors()) {
                System.out.println("Errores de validación encontrados:");
                result.getAllErrors().forEach(error -> {
                    System.out.println("Campo: " + error.getObjectName() + " - Mensaje: " + error.getDefaultMessage());
                    if (error instanceof FieldError) {
                        FieldError fieldError = (FieldError) error;
                        System.out.println("Campo específico: " + fieldError.getField() + " - Valor rechazado: " + fieldError.getRejectedValue());
                    }
                });

                model.addAttribute("asistenciaId", asistenciaId);
                model.addAttribute("usuario", usuario);

                // Repoblar con el objeto original en caso de edición
                if (!isCreation) {
                    // Mantener los valores originales para los campos no modificados
                    originalObservacion.setNivelUrgencia(observacionForm.getNivelUrgencia());
                    originalObservacion.setDescripcion(observacionForm.getDescripcion());
                    originalObservacion.setFotoUrl(observacionForm.getFotoUrl() != null ? observacionForm.getFotoUrl() : originalObservacion.getFotoUrl());
                    model.addAttribute("observacionForm", originalObservacion); // Usar el objeto original con los valores actualizados
                } else {
                    model.addAttribute("observacionForm", observacionForm); // Usar el formulario enviado para creación
                }
                model.addAttribute("org.springframework.validation.BindingResult.observacionForm", result);
                return isCreation ? "Coordinador/observacionNewForm" : "Coordinador/observacionEditForm";
            }

            // Manejar la subida de la foto al bucket S3
            if (foto != null && !foto.isEmpty()) {
                System.out.println("Intentando subir archivo a S3 para observación...");
                String uploadResult = s3Service.uploadFile(foto);
                System.out.println("Resultado de la subida: " + uploadResult);

                if (uploadResult != null && uploadResult.contains("URL:")) {
                    String fotoUrl = uploadResult.substring(uploadResult.indexOf("URL: ") + 5).trim();
                    observacionForm.setFotoUrl(fotoUrl);
                } else if (uploadResult != null && uploadResult.contains("Error:")) {
                    System.out.println("Error detectado en el resultado: " + uploadResult);
                    if (isCreation) {
                        observacionForm.setFotoUrl(defaultFotoUrl);
                    } else {
                        observacionForm.setFotoUrl(existingFotoUrl != null ? existingFotoUrl : defaultFotoUrl);
                    }
                    redirectAttributes.addFlashAttribute("message", "Error al subir la foto de la observación: " + uploadResult + ". Se usó una imagen por defecto.");
                    redirectAttributes.addFlashAttribute("messageType", "error");
                    observacionRepository.save(observacionForm);
                    return "redirect:/coordinador/observaciones";
                } else {
                    System.out.println("Resultado inválido de la subida: " + uploadResult);
                    if (isCreation) {
                        observacionForm.setFotoUrl(defaultFotoUrl);
                    } else {
                        observacionForm.setFotoUrl(existingFotoUrl != null ? existingFotoUrl : defaultFotoUrl);
                    }
                    redirectAttributes.addFlashAttribute("message", "Error desconocido al subir la foto. Se usó una imagen por defecto.");
                    redirectAttributes.addFlashAttribute("messageType", "error");
                    observacionRepository.save(observacionForm);
                    return "redirect:/coordinador/observaciones";
                }
            } else if (!isCreation && (foto == null || foto.isEmpty())) {
                observacionForm.setFotoUrl(existingFotoUrl != null ? existingFotoUrl : defaultFotoUrl);
            } else if (isCreation) {
                observacionForm.setFotoUrl(defaultFotoUrl);
            }

            // Asegurarse de que los campos no modificados se mantengan antes de guardar
            if (!isCreation) {
                observacionForm.setCoordinador(originalObservacion.getCoordinador());
                observacionForm.setEspacioDeportivo(originalObservacion.getEspacioDeportivo());
                observacionForm.setFechaCreacion(originalObservacion.getFechaCreacion());
                observacionForm.setFechaActualizacion(LocalDateTime.now());
                observacionForm.setEstado(originalObservacion.getEstado());
                observacionForm.setComentarioAdministrador(originalObservacion.getComentarioAdministrador());
            }

            // Guardar la observación
            observacionRepository.save(observacionForm);
            redirectAttributes.addFlashAttribute("message", "Observación guardada exitosamente");
            redirectAttributes.addFlashAttribute("messageType", "success");
            return "redirect:/coordinador/observaciones";
        }        // Exportar observaciones a Excel
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
                String[] headers = {"Fecha", "Espacio Deportivo", "Establecimiento", "Descripción", "Estado"};
                
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
                    
                    Cell descripcionCell = row.createCell(3);
                    descripcionCell.setCellValue(observacion.getDescripcion());
                    descripcionCell.setCellStyle(dataStyle);
                    
                    Cell estadoCell = row.createCell(4);
                    estadoCell.setCellValue(getEstadoObservacionTexto(observacion.getEstado()));
                    estadoCell.setCellStyle(dataStyle);
                }

                // Ajustar ancho de columnas
                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                    // Asegurar un ancho mínimo
                    if (sheet.getColumnWidth(i) < 3000) {
                        sheet.setColumnWidth(i, 3000);
                    }
                }

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                workbook.write(outputStream);
                workbook.close();

                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                responseHeaders.setContentDispositionFormData("attachment", "observaciones.xlsx");

                return ResponseEntity.ok()
                        .headers(responseHeaders)
                        .body(outputStream.toByteArray());

            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }

        // Exportar observaciones a PDF
        @GetMapping("/observaciones/export/pdf")
        public ResponseEntity<byte[]> exportarObservacionesPdf() {
            try {
                List<Observacion> observaciones = observacionRepository.findAll();
                
                Document document = new Document(PageSize.A4.rotate()); // Formato paisaje
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                PdfWriter.getInstance(document, outputStream);
                
                document.open();
                
                // Título
                com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
                Paragraph title = new Paragraph("Reporte de Observaciones", titleFont);
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
                float[] columnWidths = {2f, 3f, 3f, 4f, 2f};
                table.setWidths(columnWidths);
                
                // Estilo para encabezados
                com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
                BaseColor headerColor = new BaseColor(52, 58, 64);
                
                // Agregar encabezados
                String[] headers = {"Fecha", "Espacio Deportivo", "Establecimiento", "Descripción", "Estado"};
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
                for (Observacion observacion : observaciones) {
                    // Fecha
                    PdfPCell dateCell = new PdfPCell(new Phrase(
                        observacion.getFechaCreacion().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")), dataFont));
                    dateCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    dateCell.setPadding(5);
                    table.addCell(dateCell);
                    
                    // Espacio Deportivo
                    PdfPCell espacioCell = new PdfPCell(new Phrase(observacion.getEspacioDeportivo().getNombre(), dataFont));
                    espacioCell.setPadding(5);
                    table.addCell(espacioCell);
                    
                    // Establecimiento
                    PdfPCell establecimientoCell = new PdfPCell(new Phrase(
                        observacion.getEspacioDeportivo().getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre(), dataFont));
                    establecimientoCell.setPadding(5);
                    table.addCell(establecimientoCell);
                    
                    // Descripción
                    PdfPCell descripcionCell = new PdfPCell(new Phrase(observacion.getDescripcion(), dataFont));
                    descripcionCell.setPadding(5);
                    table.addCell(descripcionCell);
                    
                    // Estado
                    PdfPCell estadoCell = new PdfPCell(new Phrase(getEstadoObservacionTexto(observacion.getEstado()), dataFont));
                    estadoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    estadoCell.setPadding(5);
                    table.addCell(estadoCell);
                }
                
                document.add(table);
                document.close();
                
                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.setContentType(MediaType.APPLICATION_PDF);
                responseHeaders.setContentDispositionFormData("attachment", "observaciones.pdf");
                
                return ResponseEntity.ok()
                        .headers(responseHeaders)
                        .body(outputStream.toByteArray());
                        
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }        // Método auxiliar para convertir estado de observación a texto legible
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

        @GetMapping("/calendario")
        public ResponseEntity<List<Asistencia>> getAsistenciasParaCalendario(
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
                @RequestParam int userId) {

            List<Asistencia> asistencias = asistenciaRepository.findForCalendarRangeExcludingCanceled(start, end, userId);
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
        */

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

        @GetMapping("/asistencia/{asistenciaId}")
        @ResponseBody
        public ResponseEntity<Asistencia> obtenerDetalleAsistencia(@PathVariable Integer asistenciaId, HttpSession session) {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            Optional<Asistencia> asistenciaOpt = asistenciaRepository.findById(asistenciaId);
            if (asistenciaOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Asistencia asistencia = asistenciaOpt.get();
            
            // Verificar que la asistencia pertenece al coordinador logueado
            if (!asistencia.getCoordinador().getUsuarioId().equals(usuario.getUsuarioId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            return ResponseEntity.ok(asistencia);
        }
        
        @PostMapping("/asistencia/{asistenciaId}/cancelar")
        @ResponseBody
        public ResponseEntity<String> cancelarAsistencia(@PathVariable Integer asistenciaId, 
                                                        @RequestBody Map<String, String> request,
                                                        HttpSession session) {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
            }
            
            Optional<Asistencia> asistenciaOpt = asistenciaRepository.findById(asistenciaId);
            if (asistenciaOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Asistencia no encontrada");
            }
            
            Asistencia asistencia = asistenciaOpt.get();
            
            // Verificar que la asistencia pertenece al coordinador logueado
            if (!asistencia.getCoordinador().getUsuarioId().equals(usuario.getUsuarioId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No tiene permisos para cancelar esta asistencia");
            }
            
            // Verificar que la asistencia esté en estado pendiente
            if (asistencia.getEstadoEntrada() != Asistencia.EstadoEntrada.pendiente) {
                return ResponseEntity.badRequest().body("Solo se pueden cancelar asistencias en estado pendiente");
            }
            
            // Verificar que sea por lo menos para el día siguiente (24 horas)
            LocalDateTime ahora = LocalDateTime.now();
            LocalDateTime horarioAsistencia = asistencia.getHorarioEntrada();
            if (horarioAsistencia.isBefore(ahora.plusHours(24))) {
                return ResponseEntity.badRequest().body("Solo se pueden cancelar asistencias programadas para dentro de 24 horas o más");
            }
            
            String motivo = request.get("motivo");
            if (motivo == null || motivo.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Debe proporcionar un motivo para la cancelación");
            }
            
            // Actualizar la asistencia
            asistencia.setEstadoEntrada(Asistencia.EstadoEntrada.cancelada);
            asistencia.setObservacionAsistencia(motivo);
            asistenciaRepository.save(asistencia);
            
            // Crear notificación para el administrador
            crearNotificacionCancelacion(asistencia, motivo);
            
            return ResponseEntity.ok("Asistencia cancelada exitosamente");
        }
        
        private void crearNotificacionCancelacion(Asistencia asistencia, String motivo) {
            try {
                Notificacion notificacion = new Notificacion();
                notificacion.setUsuario(asistencia.getAdministrador());
                notificacion.setTituloNotificacion("Asistencia Cancelada");
                notificacion.setMensaje(String.format("El coordinador %s %s ha cancelado su asistencia programada para el %s. Motivo: %s",
                        asistencia.getCoordinador().getNombres(),
                        asistencia.getCoordinador().getApellidos(),
                        asistencia.getHorarioEntrada().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                        motivo));
                notificacion.setUrlRedireccion(""); // URL vacía como solicitaste
                notificacion.setEstado(Notificacion.Estado.no_leido);
                notificacion.setFechaCreacion(LocalDateTime.now());
                
                // Buscar el tipo de notificación para cancelación de asistencia (id 5 según los datos de inicialización)
                TipoNotificacion tipoNotificacion = tipoNotificacionRepository.findById(5)
                        .orElse(null);
                if (tipoNotificacion != null) {
                    notificacion.setTipoNotificacion(tipoNotificacion);
                    notificacionRepository.save(notificacion);
                }
            } catch (Exception e) {
                // Solo log del error, no fallar la cancelación por esto
                System.err.println("Error al crear notificación de cancelación: " + e.getMessage());
            }
        }
    }
