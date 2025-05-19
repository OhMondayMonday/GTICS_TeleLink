package com.example.telelink.controller;

import com.example.telelink.dto.vecino.PagoRequest;
import com.example.telelink.entity.*;
import com.example.telelink.repository.EspacioDeportivoRepository;
import com.example.telelink.repository.PagoRepository;
import com.example.telelink.repository.ReservaRepository;
import com.example.telelink.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
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

import java.time.LocalDate;
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
    private EspacioDeportivoRepository spacioDeportivoRepository;

    @GetMapping("/inicio")
    public String mostrarInicio(Model model, HttpSession session) {
        // Buscar usuario ID 6 (usuario por defecto)
        Usuario usuario = (Usuario) session.getAttribute("usuario");

        // Pasar datos al modelo
        model.addAttribute("usuario", usuario);
        model.addAttribute("activeItem", "inicio");

        return "vecino/vecino-index";
    }

    @GetMapping("/reservas/{id}")
    public String mostrarReservation(@PathVariable Integer id, Model model) {
        EspacioDeportivo espacio = espacioDeportivoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el espacio deportivo"));

        model.addAttribute("espacio", espacio);

        return "Vecino/vecino-servicioDeportivo";
    }

    @GetMapping("/reservas/futbol")
    public String mostrarFutbolReservation(Model model) {
        // Aquí puedes agregar cualquier lógica que necesites
        return "Vecino/vecino-futbol";
    }

    @GetMapping("/reservas/piscina")
    public String mostrarPiscinaReservation(Model model) {
        // Aquí puedes agregar cualquier lógica que necesites
        return "Vecino/vecino-piscina";
    }

    @GetMapping("/reservas/multiple")
    public String mostrarMultipleReservation(Model model) {
        // Aquí puedes agregar cualquier lógica que necesites
        return "Vecino/vecino-multiple";
    }

    @GetMapping("/reservas/gym")
    public String mostrarGymReservation(Model model) {
        // Aquí puedes agregar cualquier lógica que necesites
        return "Vecino/vecino-gym";
    }

    @GetMapping("/perfil")
    public String mostrarPerfil(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("currentUser");
        if (usuario == null) {
            return "redirect:/usuarios/inicio";
        }
        model.addAttribute("usuario", usuario);
        model.addAttribute("activeItem", "perfil");
        return "Vecino/vecino-perfil";
    }

    @GetMapping("")
    public String listarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioRepository.findAllByOrderByUsuarioIdAsc());
        return "lista-usuarios";
    }


    @GetMapping("/pagos")
    public String mostrarPagos(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("currentUser");
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
        Usuario usuario = (Usuario) session.getAttribute("currentUser");
        if (usuario == null) {
            return "redirect:/usuarios/inicio";
        }
        List<Reserva> reservas = reservaRepository.findByUsuarioOrderByInicioReservaDesc(usuario);
        model.addAttribute("usuario", usuario);
        model.addAttribute("reservas", reservas); // importante para mostrarlos en la vista
        model.addAttribute("activeItem", "reservas");
        return "Vecino/vecino-mis-reservas";
    }

    @PostMapping("/reserva/cancelar/{id}")
    public String cancelarReserva(@PathVariable("id") Integer id,
                                  @RequestParam(required = false) String razon,
                                  RedirectAttributes redirectAttributes,
                                  HttpSession session) {

        // Obtener el usuario actual de la sesión
        Usuario usuarioActual = (Usuario) session.getAttribute("currentUser");
        if (usuarioActual == null) {
            return "redirect:/usuarios/inicio";
        }

        // Buscar la reserva por ID
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));

        // Verificar que la reserva pertenezca al usuario actual
        if (!reserva.getUsuario().getUsuarioId().equals(usuarioActual.getUsuarioId())) {
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
        Usuario usuario = (Usuario) session.getAttribute("currentUser");
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
                        tipoServicio.toLowerCase().contains("multifuncional");
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
        Usuario usuario = (Usuario) session.getAttribute("currentUser");
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

    @GetMapping("/reservar")
    public String mostrarReservar(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("currentUser");
        if (usuario == null) {
            return "redirect:/usuarios/inicio";
        }

        List<Reserva> reservasUsuario = reservaRepository.findByUsuario_UsuarioId(usuario.getUsuarioId());
        List<EspacioDeportivo> espacioDeportivos = espacioDeportivoRepository.findAll();

        model.addAttribute("usuario", usuario);
        model.addAttribute("reservas", reservasUsuario);
        model.addAttribute("espacios", espacioDeportivos);

        return "Vecino/vecino-reservar";
    }


    @PostMapping("/confirmar-reserva")
    public String confirmarReserva(
            @RequestParam("espacio") Integer espacioId,
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
            return "redirect:/usuario/mis-reservas";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al procesar la reserva: " + e.getMessage());
            return "redirect:/usuario/reservar";
        }
    }

    // Método auxiliar para guardar el comprobante
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
    }

    @GetMapping("/reservasCalendario/{id}")
    public String verCalendarioReservas(@PathVariable("id") Integer id, Model model) {
        model.addAttribute("espacioId", id);
        return "vecino/reservas-calendario";
    }
}
