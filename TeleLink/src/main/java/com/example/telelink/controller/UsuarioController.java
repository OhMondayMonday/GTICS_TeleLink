package com.example.telelink.controller;

import com.example.telelink.entity.EspacioDeportivo;
import com.example.telelink.entity.Pago;
import com.example.telelink.entity.Reserva;
import com.example.telelink.entity.Usuario;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

        return "Vecino/vecino-servicioDeportivo"; // Nombre de la vista Thymeleaf para la cancha de fútbol
    }

    @GetMapping("/reservas/futbol")
    public String mostrarFutbolReservation(Model model) {
        // Aquí puedes agregar cualquier lógica que necesites
        return "Vecino/vecino-futbol"; // Nombre de la vista Thymeleaf para la cancha de fútbol
    }

    @GetMapping("/reservas/piscina")
    public String mostrarPiscinaReservation(Model model) {
        // Aquí puedes agregar cualquier lógica que necesites
        return "Vecino/vecino-piscina"; // Nombre de la vista Thymeleaf para la piscina
    }
    @GetMapping("/reservas/multiple")
    public String mostrarMultipleReservation(Model model) {
        // Aquí puedes agregar cualquier lógica que necesites
        return "Vecino/vecino-multiple"; // Nombre de la vista Thymeleaf para la cancha múltiple
    }

    @GetMapping("/reservas/gym")
    public String mostrarGymReservation(Model model) {
        // Aquí puedes agregar cualquier lógica que necesites
        return "Vecino/vecino-gym"; // Nombre de la vista Thymeleaf para la cancha múltiple
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

}