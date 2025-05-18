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

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.io.File;
import java.io.IOException;

import java.io.*;
import java.net.*;
import org.springframework.http.ResponseEntity;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private EspacioDeportivoRepository espacioDeportivoRepository;

    @GetMapping("/inicio")
    public String mostrarInicio(Model model, HttpSession session) {
        // Buscar usuario ID 6 (usuario por defecto)
        Integer userId = 6;
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Almacenar el objeto Usuario en la sesión
        session.setAttribute("currentUser", usuario);

        // Pasar datos al modelo
        model.addAttribute("usuario", usuario);
        model.addAttribute("activeItem", "inicio");

        return "Vecino/vecino-index";
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
        List<Usuario> usuarios = usuarioRepository.findAllByOrderByUsuarioIdAsc();
        model.addAttribute("usuarios", usuarios);
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
        model.addAttribute("espacios", espacios); // importante para mostrarlos en la vista
        model.addAttribute("activeItem", "canchas");
        return "Vecino/vecino-cancha";
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

}